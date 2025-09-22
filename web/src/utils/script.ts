import i18n from '@/locales';
const scriptMain = {
  parseIntIpNum(num: any) {
    // 格式化ip数字
    let data = parseInt(num);
    if (data > 255) {
      data = 255;
    }
    return data || 0;
  },
  createRandomStr(len: any, type: any) {
    // 生成随机字母数字 长度/类型
    len = len || 32;
    let chars = '';
    if (type.indexOf('A') > -1) {
      chars += 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    }
    if (type.indexOf('a') > -1) {
      chars += 'abcdefghijklmnopqrstuvwxyz';
    }
    if (type.indexOf('0') > -1) {
      chars += '0123456789';
    }
    let pwd = '';
    for (let i = 0; i < len; i++) {
      pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pwd;
  },
  validateName(rule: any, value: any, callback: any) {
    // 验证名称
    if (value === '') {
      callback(new Error(i18n.global.t('utils.script.inputContent')));
    } else if (value.length > 64) {
      callback(new Error(i18n.global.t('utils.script.contentLength')));
    } else if (!/^[a-zA-Z0-9\u4e00-\u9fa5\_\-\.\@]{0,}$/.test(value)) {
      callback(new Error(i18n.global.t('utils.script.contentFormat')));
    } else {
      callback();
    }
  },
  validateAccount(rule: any, value: any, callback: any) {
    // 验证账号
    if (value === '') {
      callback(new Error(i18n.global.t('utils.script.inputContent')));
    } else if (value.length > 64) {
      callback(new Error(i18n.global.t('utils.script.contentLength')));
    } else if (!/^[a-zA-Z0-9][a-zA-Z0-9\@\_\-\.]{0,}$/.test(value)) {
      callback(new Error(i18n.global.t('utils.script.contentFormat')));
    } else {
      callback();
    }
  },
  validateProt(rule: any, value: any, callback: any) {
    // 验证端口
    if (value === '') {
      callback(new Error(i18n.global.t('utils.script.inputPort')));
    } else if (value * 1 > 65535 || value * 1 < 1) {
      callback(new Error(i18n.global.t('utils.script.portRange')));
    } else if (!/^[0-9]{0,5}$/.test(value)) {
      callback(new Error(i18n.global.t('utils.script.portFormat')));
    } else {
      callback();
    }
  },
  validateEmail(rule: any, value: any, callback: any) {
    // 验证邮箱
    if (value === '') {
      callback(new Error(i18n.global.t('utils.script.inputEmail')));
    } else if (!/^(\w-*\.*)+@(\w-?)+(\.\w{2,})+$/.test(value)) {
      callback(new Error(i18n.global.t('utils.script.emailFormat')));
    } else {
      callback();
    }
  },
  validatePhone(rule: any, value: any, callback: any) {
    // 验证手机号
    if (value === '') {
      callback(new Error(i18n.global.t('utils.script.inputPhone')));
    } else if (!/^1[3456789]\d{9}$/.test(value)) {
      callback(new Error(i18n.global.t('utils.script.phoneFormat')));
    } else {
      callback();
    }
  },

  getICMP() {
    const data = [
      // ICMP
      {
        name: i18n.global.t('utils.script.all'),
        value: '0',
      },
      {
        name: 'Echo',
        value: '1',
      },
      {
        name: 'Echo reply',
        value: '2',
      },
      {
        name: 'Fragment need DF set',
        value: '3',
      },
      {
        name: 'Host redirect',
        value: '4',
      },
      {
        name: 'Host TOS redirect',
        value: '5',
      },
      {
        name: 'Host unreachable',
        value: '6',
      },
      {
        name: 'Information reply',
        value: '7',
      },
      {
        name: 'Information request',
        value: '8',
      },
      {
        name: 'Net redirect',
        value: '9',
      },
      {
        name: 'Net TOS redirect',
        value: '10',
      },
      {
        name: 'Net unreachable',
        value: '11',
      },
      {
        name: 'Parameter problem',
        value: '12',
      },
      {
        name: 'Port unreachable',
        value: '13',
      },
      {
        name: 'Protocol unreachable',
        value: '14',
      },
      {
        name: 'Reassembly timeout',
        value: '15',
      },
      {
        name: 'Source quench',
        value: '16',
      },
      {
        name: 'Source route failed',
        value: '17',
      },
      {
        name: 'Timestamp reply',
        value: '18',
      },
      {
        name: 'Timestamp request',
        value: '19',
      },
      {
        name: 'TTL exceeded',
        value: '20',
      },
    ];
    return data;
  },
};

export default scriptMain;
