package org.entermediadb.server;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.HttpSharedConnection;
import org.openedit.util.StringEncryption;

/**
 * Server-to-server access through ordinary user accounts, so the usual permissions, groups and logs
 * apply.
 *
 * <ul>
 * <li>Root (eme.world): every emeserver has a user {@code emeserver_<id>} in group
 * {@value #SERVERS_GROUP}, set as the emeserver's owner. Its login key is emeserver.serverkey; when
 * that is empty it is filled from the key the server publishes at
 * {@code <servermediadburl>/services/serverkey.json}.</li>
 * <li>Child (e.g. impactbank.world): a user {@value #ROOT_SERVER_USER} in group
 * {@value #ROOT_SERVER_GROUP} whose login key is the root's public key (catalog setting
 * rootserverkey).</li>
 * </ul>
 *
 * A server signs in to the other with the normal key login (token.json grant_type=key) using its
 * own key pair from .env (bin/serverkey.sh), and then calls the other server with the Bearer token
 * like any user. Server keys are stored in userkey with the key's fingerprint as id, which is
 * EME_SERVER_KEY_ID in .env.
 */
public class EmeServerAccountManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(EmeServerAccountManager.class);

	public static final String SERVER_USER_PREFIX = "emeserver_";
	public static final String SERVERS_GROUP = "emeservers";
	public static final String ROOT_SERVER_USER = "rootserver";
	public static final String ROOT_SERVER_GROUP = "rootserver";

	public static final String ENV_PRIVATE_KEY = "EME_SERVER_PRIVATE_KEY";
	public static final String ENV_PUBLIC_KEY = "EME_SERVER_PUBLIC_KEY";
	public static final String ENV_KEY_ID = "EME_SERVER_KEY_ID";

	/**
	 * Bearer tokens this server holds for other servers: "baseurl|username" -> {token, expiresAtMillis}
	 */
	protected static final Map<String, Object[]> TOKENS = new ConcurrentHashMap<String, Object[]>();

	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}

	protected UserManager getUserManager()
	{
		return getMediaArchive().getUserManager();
	}

	protected StringEncryption getStringEncryption()
	{
		return getUserManager().getStringEncryption();
	}

	public static String serverUserId(String inServerId)
	{
		return SERVER_USER_PREFIX + inServerId;
	}

	/**
	 * Same fingerprint as bin/serverkey.sh: first 16 hex characters of SHA-256 over the public key DER.
	 */
	public static String keyId(String inPublicKey)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(Base64.getUrlDecoder().decode(inPublicKey));
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 8; i++)
			{
				hex.append(String.format("%02x", digest[i]));
			}
			return hex.toString();
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Accounts
	// ---------------------------------------------------------------------------------------------

	/**
	 * Root side: makes sure the emeserver has its server user, that the user is its owner, and that the
	 * user's login key is the key the server publishes. Safe to run on every save.
	 *
	 * @return the server user
	 */
	public User ensureServerAccount(Data inServer)
	{
		String userid = serverUserId(inServer.getId());
		User user = ensureUser(userid, inServer.getName(), SERVERS_GROUP, "EME Servers");

		// The editor fills an empty owner with whoever saved the record, so always put the server user back
		if (!userid.equals(inServer.get("owner")))
		{
			inServer.setValue("owner", userid);
			getMediaArchive().getSearcher("emeserver").saveData(inServer);
		}

		String publickey = inServer.get("serverkey");
		if (publickey == null || publickey.trim().isEmpty())
		{
			// Not entered by an admin: take the key the server publishes and keep it on the record, where it
			// can be
			// checked against the server's bin/serverkey.sh --show and changed
			String url = inServer.get("servermediadburl");
			publickey = url == null || url.trim().isEmpty() ? null : fetchPublishedKey(url);
			if (publickey == null)
			{
				log.info("emeserver " + inServer.getId() + " has no serverkey and none could be loaded; " + userid + " cannot sign in");
			}
			else
			{
				inServer.setValue("serverkey", publickey);
				getMediaArchive().getSearcher("emeserver").saveData(inServer);
			}
		}
		syncLoginKey(userid, publickey, "EME Server " + inServer.getId()); // no key disables its login
		if (publickey != null && getStringEncryption().isValidPublicKey(publickey.trim()))
		{
			log.info("emeserver " + inServer.getId() + " signs in as " + userid + " with key " + keyId(publickey.trim()));
		}
		return user;
	}

	/**
	 * @return the public key at inMediaDbUrl/services/serverkey.json, or null when it cannot be loaded
	 */
	public String fetchPublishedKey(String inMediaDbUrl)
	{
		String url = trimSlash(inMediaDbUrl) + "/services/serverkey.json";
		HttpSharedConnection connection = new HttpSharedConnection();
		CloseableHttpResponse resp = null;
		try
		{
			resp = connection.sharedGet(url);
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.error(url + " returned " + resp.getStatusLine().getStatusCode());
				return null;
			}
			JSONObject json = (JSONObject) new org.json.simple.parser.JSONParser().parse(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8));
			String publickey = (String) json.get("publickey");
			if (publickey == null || !getStringEncryption().isValidPublicKey(publickey))
			{
				log.error(url + " has no valid publickey");
				return null;
			}
			return publickey;
		}
		catch (Exception e)
		{
			log.error("Could not load " + url, e);
			return null;
		}
		finally
		{
			connection.release(resp);
		}
	}

	/**
	 * Child side: makes sure the {@value #ROOT_SERVER_USER} user exists and its login key is the
	 * catalog setting rootserverkey. Without a valid setting the user has no enabled key, so the root
	 * cannot sign in.
	 */
	public User ensureRootServerAccount()
	{
		User user = ensureUser(ROOT_SERVER_USER, "Root Server", ROOT_SERVER_GROUP, "Root Server");
		syncLoginKey(ROOT_SERVER_USER, getMediaArchive().getCatalogSettingValue("rootserverkey"), "Root server");
		return user;
	}

	protected User ensureUser(String inUserId, String inName, String inGroupId, String inGroupName)
	{
		UserManager users = getUserManager();
		User user = users.getUser(inUserId);
		boolean changed = false;
		if (user == null)
		{
			user = users.createUser(inUserId);
			user.setEnabled(true);
			changed = true;
			log.info("Created server user " + inUserId);
		}
		if (inName != null && !inName.equals(user.getFirstName()))
		{
			user.setFirstName(inName);
			changed = true;
		}
		Group group = users.getGroup(inGroupId);
		if (group == null)
		{
			group = users.createGroup(inGroupId, inGroupName);
			users.saveGroup(group);
		}
		if (!user.isInGroup(group))
		{
			user.addGroup(group);
			changed = true;
		}
		if (changed)
		{
			users.saveUser(user);
		}
		return user;
	}

	/**
	 * Makes inPublicKey the only enabled public key of inUserId (id = its fingerprint). A blank or
	 * invalid key disables all of them.
	 */
	protected void syncLoginKey(String inUserId, String inPublicKey, String inLabel)
	{
		Searcher keys = getMediaArchive().getSearcherManager().getSearcher("system", "userkey");
		String current = null;
		if (inPublicKey != null && !inPublicKey.trim().isEmpty())
		{
			String publickey = inPublicKey.trim();
			if (getStringEncryption().isValidPublicKey(publickey))
			{
				current = keyId(publickey);
				Data key = (Data) keys.searchById(current);
				if (key != null && !inUserId.equals(key.get("userid")))
				{
					log.error("Key " + current + " already belongs to " + key.get("userid") + "; not giving it to " + inUserId);
					current = null;
				}
				else
				{
					if (key == null)
					{
						key = keys.createNewData();
						key.setId(current);
						key.setValue("userid", inUserId);
						key.setValue("created", new Date());
					}
					key.setValue("label", inLabel);
					key.setValue("keytype", "publickey");
					key.setValue("algorithm", getStringEncryption().getPublicKeyAlgorithm(publickey));
					key.setValue("publickey", publickey);
					key.setValue("enabled", true);
					keys.saveData(key);
				}
			}
			else
			{
				log.error("Public key for " + inUserId + " is not a valid Ed25519/P-256 key; its login is disabled");
			}
		}
		Collection<Data> existing = keys.query().exact("userid", inUserId).exact("keytype", "publickey").search();
		for (Data old : existing)
		{
			if (!old.getId().equals(current) && Boolean.parseBoolean(old.get("enabled")))
			{
				old.setValue("enabled", false);
				keys.saveData(old);
				log.info("Disabled old login key " + old.getId() + " of " + inUserId);
			}
		}
	}

	// ---------------------------------------------------------------------------------------------
	// This server's own key, from .env (bin/serverkey.sh)
	// ---------------------------------------------------------------------------------------------

	public String getOwnEnv(String inKey)
	{
		File envFile = new File(getStringEncryption().getRootDirectory(), "../.env");
		if (!envFile.exists())
		{
			return null;
		}
		try (FileInputStream in = new FileInputStream(envFile))
		{
			Properties env = new Properties();
			env.load(in);
			String value = env.getProperty(inKey);
			return value == null || value.trim().isEmpty() ? null : value.trim();
		}
		catch (Exception e)
		{
			throw new OpenEditException("Could not read " + envFile, e);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Calling another server as a user
	// ---------------------------------------------------------------------------------------------

	/**
	 * Signs in to the server at inBaseUrl (its mediadb URL) as inUserName with this server's key and
	 * returns the Bearer token, reusing a cached one until an hour before it expires.
	 */
	public String getAccessToken(String inBaseUrl, String inUserName, boolean inForce)
	{
		String base = trimSlash(inBaseUrl);
		String cacheKey = base + "|" + inUserName;
		Object[] cached = TOKENS.get(cacheKey);
		if (!inForce && cached != null && (Long) cached[1] > System.currentTimeMillis())
		{
			return (String) cached[0];
		}
		String privateKey = getOwnEnv(ENV_PRIVATE_KEY);
		String keyid = getOwnEnv(ENV_KEY_ID);
		if (privateKey == null || keyid == null)
		{
			throw new OpenEditException(ENV_PRIVATE_KEY + " / " + ENV_KEY_ID + " missing in .env; run bin/serverkey.sh");
		}
		StringEncryption encryption = getStringEncryption();
		String timestamp = String.valueOf(System.currentTimeMillis());
		String nonce = encryption.randomCharacterString();
		String signature = encryption.sign(privateKey, encryption.createSignedMessage(inUserName, keyid, timestamp, nonce));

		Map<String, String> fields = new HashMap<String, String>();
		fields.put("grant_type", "key");
		fields.put("username", inUserName);
		fields.put("key_id", keyid);
		fields.put("timestamp", timestamp);
		fields.put("nonce", nonce);
		fields.put("signature", signature);

		HttpSharedConnection connection = new HttpSharedConnection();
		CloseableHttpResponse resp = null;
		try
		{
			resp = connection.sharedPost(base + "/services/authentication/token.json", fields);
			int status = resp.getStatusLine().getStatusCode();
			String text = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
			if (status != 200)
			{
				throw new OpenEditException("Sign-in to " + base + " as " + inUserName + " refused [" + status + "]: " + text);
			}
			JSONObject json = (JSONObject) new org.json.simple.parser.JSONParser().parse(text);
			String token = (String) json.get("access_token");
			if (token == null || token.isEmpty())
			{
				throw new OpenEditException("Sign-in to " + base + " as " + inUserName + " returned no token: " + text);
			}
			long expiresIn = json.get("expires_in") instanceof Number ? ((Number) json.get("expires_in")).longValue() : 3600L;
			TOKENS.put(cacheKey, new Object[] {token, System.currentTimeMillis() + Math.max(60L, expiresIn - 3600L) * 1000L});
			return token;
		}
		catch (OpenEditException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new OpenEditException("Could not sign in to " + base + " as " + inUserName, e);
		}
		finally
		{
			connection.release(resp);
		}
	}

	/**
	 * POSTs inBody as JSON to inBaseUrl + inPath as inUserName. Signs in again once when the token was
	 * refused.
	 *
	 * @return {status, response body}
	 */
	public Object[] postJson(String inBaseUrl, String inPath, String inUserName, JSONObject inBody)
	{
		String url = trimSlash(inBaseUrl) + inPath;
		for (int attempt = 0; attempt < 2; attempt++)
		{
			String token = getAccessToken(inBaseUrl, inUserName, attempt > 0);
			HttpSharedConnection connection = new HttpSharedConnection();
			CloseableHttpResponse resp = null;
			try
			{
				HttpPost post = new HttpPost(url);
				post.setHeader("Authorization", "Bearer " + token);
				post.setHeader("X-tokentype", "entermedia");
				post.setEntity(new StringEntity(inBody.toJSONString(), ContentType.APPLICATION_JSON));
				resp = connection.sharedPost(post);
				int status = resp.getStatusLine().getStatusCode();
				String text = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
				if ((status == 401 || status == 403) && attempt == 0)
				{
					continue; // token expired or was revoked: sign in again
				}
				return new Object[] {status, text};
			}
			catch (Exception e)
			{
				throw new OpenEditException("Request to " + url + " failed", e);
			}
			finally
			{
				connection.release(resp);
			}
		}
		throw new OpenEditException("Request to " + url + " was refused after signing in again");
	}

	protected static String trimSlash(String inUrl)
	{
		return inUrl == null ? "" : inUrl.trim().replaceAll("/+$", "");
	}
}
