package org.entermediadb.authenticate;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.google.GoogleManager;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.JSONParser;
import org.openedit.util.StringEncryption;

public class AutoLoginByToken extends BaseAutoLogin implements AutoLoginProvider
{
	private static final Log log = LogFactory.getLog(AutoLoginByToken.class);

	public GoogleManager getGoogleManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		if (catalogid == null)
		{
			catalogid = "system";
		}
		return (GoogleManager) getModuleManager().getBean(catalogid, "googleManager");
	}

	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		HttpServletRequest request = inReq.getRequest();
		if (request == null)
		{
			return null;
		}
		String accesskey = request.getHeader("X-token");
		String type = request.getHeader("X-tokentype");
		if (accesskey != null && type != null)
		{
			User user = null;

			if (type.equals("signature"))
			{
				JSONParser parser = new JSONParser();
				JSONObject json = (JSONObject) parser.parse(accesskey);
				String username = (String) json.get("username");
				String keyId = (String) json.get("key_id");
				String timestamp = (String) json.get("timestamp");
				String nonce = (String) json.get("nonce");
				String signature = (String) json.get("signature");
				if (username == null || keyId == null || timestamp == null || nonce == null || signature == null)
				{
					log.info("Missing required fields in signature login");
					return null;
				}

				if (System.currentTimeMillis() - Long.parseLong(timestamp) > StringEncryption.SIGNATURE_WINDOW_MILLIS)
				{
					log.info("Signature login expired for " + username);
					return null;
				}

				UserManager userManager = getUserManager(inReq);

				StringEncryption encryption = userManager.getStringEncryption();
				Data key = getSearcherManager().getCachedData("system", "userkey", keyId);

				boolean valid = encryption.verifyKeyLogin(username, keyId, key.get("publickey"), timestamp, nonce, signature);
				if (valid)
				{
					user = userManager.getUser(username);
				}
				else
				{
					return null;
				}
			}
			else if (type.equals("google"))
			{
				GoogleManager manager = getGoogleManager(inReq);
				Map<String, String> details = manager.getTokenDetails(accesskey);
				if (details != null)
				{
					String email = details.get("email");
					if (email != null)
					{
						user = manager.createUserIfNeeded(email);
					}
				}
			}
			if (type.equals("adminkey"))
			{
				// A trusted embedding site signs users in by email. X-token must be a valid credential (key token
				// or
				// application password) of an administrator; it used to be ignored, which let anyone log in as
				// anyone.
				UserManager usermanager = getUserManager(inReq);
				User admin = validateBearer(usermanager, accesskey);
				String email = request.getHeader("X-email");
				if (admin == null || !admin.isInGroup("administrators") || email == null)
				{
					log.info("Rejected adminkey login for " + email);
				}
				else
				{
					user = usermanager.getUserByEmail(email);
					if (user == null)
					{
						user = (User) usermanager.getUserSearcher().createNewData();
						user.setEmail(email);
						user.setEnabled(true);
						usermanager.saveUser(user);
					}
				}
			}
			else if (type.equals("entermedia"))
			{
				// X-tokentype: entermedia carries a key token or application password
				user = validateBearer(getUserManager(inReq), accesskey);
			}

			if (user != null)
			{
				AutoLoginResult result = new AutoLoginResult();
				result.setUser(user);
				return result;
			}

		}

		return null;
	}

}
