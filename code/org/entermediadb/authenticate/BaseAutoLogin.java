package org.entermediadb.authenticate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.StringEncryption;

/**
 * Validates the two kinds of bearer credentials: 7 day key tokens (grant_type=key) and application
 * passwords. Both are rows in the system "userkey" table, so revoking a row (enabled=false) takes effect at once.
 */
public abstract class BaseAutoLogin implements AutoLoginProvider
{
	static final Log log = LogFactory.getLog(BaseAutoLogin.class);

	protected ModuleManager fieldModuleManager;
	protected StringEncryption fieldStringEncryption;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getModuleManager().getBean("searcherManager");
	}

	protected UserManager getUserManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		if (catalogid == null)
		{
			catalogid = "system";
		}
		return (UserManager) getModuleManager().getBean(catalogid, "userManager");
	}

	public StringEncryption getStringEncryption()
	{
		return fieldStringEncryption;
	}

	public void setStringEncryption(StringEncryption inStringEncryption)
	{
		fieldStringEncryption = inStringEncryption;
	}

	/**
	 * @return the user for a Bearer value (key token or application password), or null
	 */
	public User validateBearer(UserManager inUserManager, String inToken)
	{
		return inUserManager.getUserByBearerToken(inToken);
	}
}
