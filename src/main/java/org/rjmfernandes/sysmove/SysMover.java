package org.rjmfernandes.sysmove;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

/**
 * 
 * @author Rui Fernandes
 * 
 */
public class SysMover extends BaseProcessorExtension
{

	private AuthenticationComponent authenticationComponent;
	private ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry)
	{
		this.serviceRegistry = serviceRegistry;
	}

	public void setAuthenticationComponent(
	        AuthenticationComponent authenticationComponent)
	{
		this.authenticationComponent = authenticationComponent;
	}

	public void move(ScriptNode docNode, ScriptNode targetFolder)
	{
		this.move(docNode.getNodeRef(), targetFolder.getNodeRef());
	}

	public void move(final NodeRef docNode, final NodeRef targetFolder)
	{
		RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{
				String user = authenticationComponent.getCurrentUserName();
				try
				{
					authenticationComponent.setSystemUserAsCurrentUser();

					movePlain(docNode, targetFolder);
				} finally
				{
					if (user != null)
						authenticationComponent.setCurrentUser(user);
				}
				return null;
			}

		};
		serviceRegistry.getTransactionService().getRetryingTransactionHelper()
		        .doInTransaction(txnWork, false, false);

	}

	private void movePlain(NodeRef docNode, NodeRef targetFolder)
	{
		NodeService nodeService = serviceRegistry.getNodeService();
		nodeService.moveNode(docNode, targetFolder,
		        ContentModel.ASSOC_CONTAINS,
		        nodeService.getPrimaryParent(docNode).getQName());
	}

}
