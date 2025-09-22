// Copyright 2024 The NEXTSTACK Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.lnjoying.justice.usermanager.config.security;

import com.lnjoying.justice.usermanager.config.filter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.Resource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
//
    private static final String[] ADMIN_ROLES = {
        //"CIS_ADMIN", "UMS_ADMIN", "ECRM_ADMIN"
        "ALL_ADMIN"
    };

    @Value("${cors.allow.origins}")
    private String allowOrigins;

    @Value("${apikey.header.name}")
    private String apiKeyHeaderName;

    @Value("${apikey.secret.header.name}")
    private String apiKeySecretHeaderName;

    @Value("${jwtkey}")
    private String jwtkey;

    @Resource
    private FormAuthenticationConfig formAuthenticationConfig;


    @Autowired
    private MecUserDetailsService mecUserDetailsService;

    @Autowired
    private LoginFailHandler loginFailHandler;

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private SmsLoginSuccessHandler smsloginSuccessHandler;

    @Autowired
    private EmailLoginSuccessHandler emailloginSuccessHandler;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        formAuthenticationConfig.configure(httpSecurity);
        httpSecurity.exceptionHandling()
                .and()
                .authorizeRequests()
                .antMatchers("/", "/AuthImpl/**", "/login", "/login.html","/css/**", "/fonts/**", "/api/ums/v1/health/**","/health/**","/img/**","/exception/**",
                        "/js/**", "/favicon.ico", "/inspector/**", "/UmsServiceImpl/**",
                        "/index.html", "/user-privacy.md", "/user-agreement.md","/error/**")
                .permitAll()
                .antMatchers("/api/ums/v1/verification/**").permitAll()
//                .antMatchers("/health/**").permitAll()
                .antMatchers(HttpMethod.POST,  "/api/ums/v1/users/registration").permitAll()
                .antMatchers(HttpMethod.PATCH,  "/api/ums/v1/users/retrieved-password").permitAll()
                .antMatchers(HttpMethod.POST,  "/api/ums/v1/users").hasAnyRole(ADMIN_ROLES)
                .antMatchers(HttpMethod.DELETE,"/api/ums/v1/users/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/api/ums/v1/bps/**").hasAnyRole(ADMIN_ROLES)
                .anyRequest()
                .authenticated()
//                .and().cors().and().csrf().ignoringAntMatchers("/login")
//                .and().cors().and().csrf()
                .and().cors().and().csrf().disable()
                .headers()
                .frameOptions().sameOrigin();
//                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity.addFilterAt(customAuthenticationFilter(),  UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterAt(smsAuthenticationFilter(),     UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterAt(emailAuthenticationFilter(),   UsernamePasswordAuthenticationFilter.class);
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter = new ApiKeyAuthenticationFilter(authenticationManager(), apiKeyHeaderName, apiKeySecretHeaderName);
        httpSecurity.addFilter(apiKeyAuthenticationFilter);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager(), jwtkey);
        httpSecurity.addFilter(jwtAuthenticationFilter);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
    {
        auth.authenticationProvider(authenticationProvider());
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(mecUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    /**
     * CorsFilter solve cross-domain issues for logout api.
     *
     * @return
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(Arrays.asList(allowOrigins.split(",")));
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("GET");
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/auth/logout", corsConfiguration);
        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

    /**
     * Define the PBKDF2 encoder with sha256.
     *
     * @return
     */
    @Bean
    public Pbkdf2PasswordEncoder passwordEncoder() {
        Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder();
        encoder.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        return encoder;
    }

    @Bean
    CustomAuthenticationFilter customAuthenticationFilter() throws Exception
    {
        CustomAuthenticationFilter filter = new CustomAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(loginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailHandler);
        filter.setFilterProcessesUrl("/api/ums/v1/auth/password/tokens");
        filter.setAuthenticationManager(authenticationManagerBean());
        return filter;
    }

    @Bean
    SmsAuthenticationFilter smsAuthenticationFilter() throws Exception
    {
        SmsAuthenticationFilter filter = new SmsAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(smsloginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailHandler);
        filter.setFilterProcessesUrl("/api/ums/v1/auth/sms/tokens");
        filter.setAuthenticationManager(authenticationManagerBean());
        return filter;
    }

    @Bean
    EmailAuthenticationFilter emailAuthenticationFilter() throws Exception
    {
        EmailAuthenticationFilter filter = new EmailAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(emailloginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailHandler);
        filter.setFilterProcessesUrl("/api/ums/v1/auth/email/tokens");
        filter.setAuthenticationManager(authenticationManagerBean());
        return filter;
    }
}
