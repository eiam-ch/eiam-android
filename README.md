# eiam-android

<p align="center">
  <img width="200" height="auto" src="/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png">
</p>

## OIDC Best Practises

✅ OIDC Flow: Authorization code flow

✅ Use PKCE

✅ Use system browser (CustomTab)

✅ Set prompt=select_account / prompt=login to ensure user-interaction while login (instead of non-interactive SSO) 

✅ Store tokens (encrypted) in keychain 

✅ No tokens in app cache (an ephemeral URLSession is used)

✅ Use certificate pinning for requests to IdP

✅ Logout: drop all tokens

✅ Error handling 

✅ Time handling access/refresh token (before expired)

