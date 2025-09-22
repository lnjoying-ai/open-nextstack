#!perl
use File::Basename;
use Cwd;
use strict;

my $tmpdir      = "d:\\temp";
my $working_dir = getcwd();

my $arch   = "amd64";
my $lang   = "zh-cn";
my $secure = 1;

my $packagepath =
"c:\\program files (x86)\\windows kits\\10\\assessment and deployment kit\\windows preinstallation environment";
my $dismrootpath =
"c:\\program files (x86)\\windows kits\\10\\assessment and deployment kit\\deployment tools\\$arch\\dism";

sub cmd {
    my $cmd    = shift;
    my $output = `$cmd`;
    if ( $? != 0 ) {
        print("EXIT CODE: $?\n");
        print("$cmd\n");
        print("$output");
        die();
    }
    return $output;
}

sub patch_wim {
    my ( $wim_file, $mount_dir, $type, $index ) = @_;
    print("patching $wim_file\n");
    cmd("dism /get-wiminfo /wimfile:\"$wim_file\"");
    my $outputs = cmd("dism /get-wiminfo /wimfile:\"$wim_file\" /english");
    my @lines   = split( /[\r\n]/, $outputs );
    foreach (@lines) {
        if ( $_ =~ /^\s*index\s*:\s*(\d+)\s*$/i ) {
            my $idx = $1;
            if ( $index eq "all" || $index eq $idx ) {
                patch_index( $wim_file, $mount_dir, $idx, $type );
            }
        }
    }

    my $new_name = $type . "-" . basename($wim_file);
    print("copying $wim_file to $new_name\n");
    cmd("copy \"$wim_file\" \"$working_dir\"\\$new_name");
}

sub patch_index {
    my ( $wim_file, $mount_dir, $index, $type ) = @_;
    print("patching $wim_file: $index\n");
    print("mouting $wim_file:$index to $mount_dir\n");
    cmd(
"dism /mount-image /imagefile:\"$wim_file\" /index:$index /mountdir:\"$mount_dir\""
    );

    if ( $type eq "pe" ) {
        my @pkgs =
          qw(lp winpe-fonts-legacy winpe-fontsupport winpe-wmi winpe-netfx winpe-scripting winpe-powershell winpe-storagewmi);
        for my $pkg (@pkgs) {
            patch_package( $mount_dir, $pkg );
        }
        if ($secure) {
            my @pkgs = qw(winpe-securestartup winpe-enhancedstorage);
            for my $pkg (@pkgs) {
                patch_package( $mount_dir, $pkg );
            }
        }
        print("setting image lang to $lang\n");
        cmd("dism /image:\"$mount_dir\" /set-allintl:$lang");
    }
    cmd(
"reg load HKLM\\OFFLINE \"$mount_dir\\windows\\system32\\config\\system\""
    );
    cmd(
"reg add \"HKEY_LOCAL_MACHINE\\OFFLINE\\ControlSet001\\Control\\Session Manager\\Memory Management\" /v PagingFiles /t REG_MULTI_SZ /d \"\" /f"
    );
    cmd("reg unload HKLM\\OFFLINE");
    if ( -e "$working_dir\\	s" ) {
        print("adding driver\n");
        cmd(
"dism /image:\"$mount_dir\" /add-driver /driver:\"$working_dir\"\\drivers /recurse"
        );
    }

    print("unmouting $mount_dir\n");
    cmd("dism /unmount-image /mountdir:\"$mount_dir\" /commit");
}

sub patch_package {
    my ( $mount_dir, $package ) = @_;
    my $pkg = basename($package);
    print("adding package $pkg\n");
    if ( -e "$packagepath\\$arch\\winpe_ocs\\$lang\\$package.cab" ) {
        print("\tadding package $package.cab\n");
        cmd(
"dism /add-package /image:\"$mount_dir\" /packagepath:\"$packagepath\\$arch\\winpe_ocs\\$lang\\$package.cab\""
        );
    }
    if ( -e "$packagepath\\$arch\\winpe_ocs\\$package.cab" ) {
        print("\tadding package $package.cab\n");
        cmd(
"dism /add-package /image:\"$mount_dir\" /packagepath:\"$packagepath\\$arch\\winpe_ocs\\$package.cab\""
        );
    }
    if ( -e "$packagepath\\$arch\\winpe_ocs\\$package-$lang.cab" ) {
        print("\tadding package $package-$lang.cab\n");
        cmd(
"dism /add-package /image:\"$mount_dir\" /packagepath:\"$packagepath\\$arch\\winpe_ocs\\$package-$lang.cab\""
        );
    }
    if ( -e "$packagepath\\$arch\\winpe_ocs\\$lang\\$package\_$lang.cab" ) {
        print("\tadding package $package\_$lang.cab\n");
        cmd(
"dism /add-package /image:\"$mount_dir\" /packagepath:\"$packagepath\\$arch\\winpe_ocs\\$lang\\$package\_$lang.cab\""
        );
    }
}

chdir $packagepath;

# 删除临时目录
if ( -e $tmpdir ) {
    print("deleting $tmpdir\n");
    cmd("rmdir /s /q $tmpdir");
}

# 新建临时目录
print("making $tmpdir\n");
cmd("md $tmpdir");
cmd("md \"$tmpdir\\mount\"");

my $wim_file;
my $wim_index = -1;
my $wim_type;
my $wim_to_delete = 0;

# 第一个参数为：镜像文件
if ( $#ARGV >= 0 ) {
    $wim_file = $ARGV[0];
}

# 第二个参数为：镜像中的索引
if ( $#ARGV >= 1 ) {
    $wim_index = $ARGV[1];
}

# 不指定镜像文件则默认处理pe镜像
if ( !$wim_file ) {
    print("copying pe\n");
    cmd("copype $arch \"$tmpdir\\$arch\"");
    $wim_file  = "$tmpdir\\$arch\\media\\sources\\boot.wim";
    $wim_index = "all";
    $wim_type  = "pe";
}
else {
    if ( !-e $wim_file ) {
        die("wim file: $wim_file not found");
    }

    # 如果是ESD文件，则转换为wim文件
    if ( $wim_file =~ /\.esd$/i ) {
        print("converting esd $wim_file to $tmpdir\\install.wim\n");
        cmd(
"dism /export-image /sourceimagefile:\"$wim_file\" /sourceindex:4 /destinationimagefile:\"$tmpdir\\install.wim\" /compress:fast /checkintegrity"
        );

        $wim_to_delete = 1;
        $wim_file      = "$tmpdir\\install.wim";

    }
    elsif ( !-w $wim_file ) {
        print("copying $wim_file file to temp folder\n");
        cmd("copy \"$wim_file\" \"$tmpdir\"");
        my $fn = basename($wim_file);
        $wim_file      = "$tmpdir\\$fn";
        $wim_to_delete = 1;
    }
    $wim_type = "win";
    if ( $wim_index == -1 ) {
        $wim_index = "all";
    }
}

print("wim file: $wim_file\n");
print("wim index: $wim_index\n");
print("wim type: $wim_type\n");

patch_wim( $wim_file, "$tmpdir\\mount", $wim_type, $wim_index );
if ($wim_to_delete) {
    unlink($wim_file);
}
