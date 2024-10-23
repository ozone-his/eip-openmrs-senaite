#
# Copyright © 2021, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

from math import log
from superset.security import SupersetSecurityManager
import logging
from flask_appbuilder.security.views import AuthOAuthView
from flask_appbuilder.baseviews import expose
import time
from flask import (
    redirect,
    request
)

class CustomAuthOAuthView(AuthOAuthView):

    @expose("/logout/")
    def logout(self, provider="keycloak", register=None):
        provider_obj = self.appbuilder.sm.oauth_remotes[provider]
        redirect_url = request.url_root.strip('/') + self.appbuilder.get_url_for_login
        url = ("logout?client_id={}&post_logout_redirect_uri={}".format(
            provider_obj.client_id,
            redirect_url
        ))

        ret = super().logout()
        time.sleep(1)

        return redirect("{}{}".format(provider_obj.api_base_url, url))


class CustomSecurityManager(SupersetSecurityManager):
    # override the logout function
    authoauthview = CustomAuthOAuthView

    def oauth_user_info(self, provider, response=None):
        logging.debug("Oauth2 provider: {0}.".format(provider))
        if provider == 'keycloak':
            me = self.appbuilder.sm.oauth_remotes[provider].get('userinfo').json()
            return {
                "username": me.get("preferred_username", ""),
                "first_name": me.get("given_name", ""),
                "last_name": me.get("family_name", ""),
                "email": me.get("email", ""),
                'roles': me.get('roles', ['Public']),
            }
        return {}
    def auth_user_oauth(self, userinfo):
        user = super(CustomSecurityManager, self).auth_user_oauth(userinfo)
        roles = [self.find_role(x) for x in userinfo['roles']]
        roles = [x for x in roles if x is not None]
        user.roles = roles
        self.update_user(user)
        return user