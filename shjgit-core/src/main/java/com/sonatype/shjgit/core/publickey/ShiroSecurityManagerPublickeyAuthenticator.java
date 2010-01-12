package com.sonatype.shjgit.core.publickey;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;

import static com.sonatype.shjgit.core.ShiroSecurityManagerUserAuthPassword.SUBJECT;

/**
 * A {@link PublickeyAuthenticator} that delegates to a Shiro {@link org.apache.shiro.mgt.SecurityManager} for
 * authentication by {@link PublicKey}.
 *
 * @author hugo@josefson.org
 */
public class ShiroSecurityManagerPublickeyAuthenticator implements PublickeyAuthenticator{

    private final SecurityManager securityManager;

    public ShiroSecurityManagerPublickeyAuthenticator(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public boolean authenticate(String username, final PublicKey key, ServerSession session) {
        try {
            final Subject subject = securityManager.getSubject();
            subject.login( new PublicKeyAuthenticationToken( username, key ) );

            session.setAttribute( SUBJECT, subject );

            return true;
        } catch( AuthenticationException e ) {
            return false;
        }

    }


}