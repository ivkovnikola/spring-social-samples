/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.showcase.config;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.UserIdSource;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.security.SocialAuthenticationServiceLocator;
import org.springframework.social.security.SocialUserDetailsService;
import org.springframework.social.security.SpringSocialAuthenticationConfigurer;
import org.springframework.social.security.SpringSocialHttpConfigurer;
import org.springframework.social.showcase.security.AuthenticationUserIdExtractor;
import org.springframework.social.showcase.security.SimpleSocialUsersDetailService;

/**
 * Security Configuration.
 * @author Craig Walls
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter{

	@Inject
	private DataSource dataSource;
	
	@Inject
	private UsersConnectionRepository usersConnectionRepository;

	@Inject
	private SocialAuthenticationServiceLocator authenticationServiceLocator;
	
	@Override
	protected void registerAuthentication(AuthenticationManagerBuilder auth) throws Exception {
		auth.jdbcAuthentication()
				.dataSource(dataSource)
				.usersByUsernameQuery("select username, password, true from Account where username = ?")
				.authoritiesByUsernameQuery("select username, 'ROLE_USER' from Account where username = ?")
				.passwordEncoder(passwordEncoder())
			.and()
				.apply(
						new SpringSocialAuthenticationConfigurer(usersConnectionRepository, socialUsersDetailsService()));
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web
			.ignoring()
				.antMatchers("/resources/**");
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.formLogin()
				.loginPage("/signin")
				.loginProcessingUrl("/signin/authenticate")
				.failureUrl("/signin?param.error=bad_credentials")
			.and()
				.logout()
					.logoutUrl("/signout")
					.deleteCookies("JSESSIONID")
			.and()
				.authorizeRequests()
					.antMatchers("/admin/**", "/favicon.ico", "/resources/**", "/auth/**", "/signin/**", "/signup/**", "/disconnect/facebook").permitAll()
					.antMatchers("/**").authenticated()
			.and()
				.rememberMe()
			.and()
				.apply(
					new SpringSocialHttpConfigurer(userIdSource(), usersConnectionRepository, authenticationServiceLocator)
				);
	}
	
	@Bean
	public SocialUserDetailsService socialUsersDetailsService() {
		return new SimpleSocialUsersDetailService(userDetailsService());
	}
	
	@Bean
	public UserIdSource userIdSource() {
		return new AuthenticationUserIdExtractor();
	}

	
	@Bean
	public PasswordEncoder passwordEncoder() {
    	return NoOpPasswordEncoder.getInstance();
	}
    
	@Bean
	public TextEncryptor textEncryptor() {
		return Encryptors.noOpText();
	}

}
