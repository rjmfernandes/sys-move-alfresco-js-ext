package org.rjmfernandes.sysmove.test;

import java.io.Serializable;
import java.util.HashMap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rjmfernandes.sysmove.SysMover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;

/**
 * 
 * @author Rui Fernandes
 * 
 */

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class SysMoveTest
{

	private NodeRef docNode;
	private NodeRef targetFolder;
	private NodeRef originalFolder;
	private NodeRef personNode;
	private String testUserName;

	@Autowired
	@Qualifier("ServiceRegistry")
	protected ServiceRegistry serviceRegistry;

	@Autowired
	@Qualifier("repositoryHelper")
	protected Repository repository;

	@Autowired
	@Qualifier("authenticationComponent")
	protected AuthenticationComponent authenticationComponent;

	@Autowired
	@Qualifier("sysMover")
	protected SysMover sysMover;

	@After
	public void tearDown()
	{
		final NodeService nodeService = serviceRegistry.getNodeService();
		final PersonService personService = serviceRegistry.getPersonService();
		RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{
				authenticationComponent.setSystemUserAsCurrentUser();
				nodeService.deleteNode(targetFolder);
				nodeService.deleteNode(originalFolder);
				personService.deletePerson(personNode);
				return null;
			}

		};
		serviceRegistry.getTransactionService().getRetryingTransactionHelper()
		        .doInTransaction(txnWork, false, false);
	}

	@Before
	public void setUp()
	{
		long currentTime = System.currentTimeMillis();
		final String originalFolderName = "original_folder_" + currentTime;
		final String documentName = "original_document_" + currentTime;
		final String targetFolderName = "target_folder_" + currentTime;
		testUserName = "test_user_" + currentTime;
		final FileFolderService fileFolderService = serviceRegistry
		        .getFileFolderService();
		final PermissionService permissionService = serviceRegistry
		        .getPermissionService();
		RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{
				authenticationComponent.setSystemUserAsCurrentUser();

				createUser();
				return null;
			}

		};
		serviceRegistry.getTransactionService().getRetryingTransactionHelper()
		        .doInTransaction(txnWork, false, false);

		authenticationComponent.setCurrentUser(testUserName);
		txnWork = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{

				authenticationComponent.setSystemUserAsCurrentUser();
				NodeRef companyHome = repository.getCompanyHome();
				originalFolder = fileFolderService.create(companyHome,
				        originalFolderName, ContentModel.TYPE_FOLDER)
				        .getNodeRef();
				permissionService.setInheritParentPermissions(originalFolder,
				        false);
				permissionService.setPermission(originalFolder, testUserName,
				        PermissionService.COORDINATOR, true);
				docNode = fileFolderService.create(originalFolder,
				        documentName, ContentModel.TYPE_CONTENT).getNodeRef();
				targetFolder = fileFolderService.create(companyHome,
				        targetFolderName, ContentModel.TYPE_FOLDER)
				        .getNodeRef();
				permissionService.setInheritParentPermissions(targetFolder,
				        false);
				permissionService.setPermission(targetFolder, testUserName,
				        PermissionService.CONSUMER, true);

				return null;
			}

		};
		serviceRegistry.getTransactionService().getRetryingTransactionHelper()
		        .doInTransaction(txnWork, false, false);

	}

	private void createUser()
	{
		PersonService personService = serviceRegistry.getPersonService();
		PermissionService permissionService = serviceRegistry
		        .getPermissionService();
		MutableAuthenticationService authenticationService = serviceRegistry
		        .getAuthenticationService();
		HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
		properties.put(ContentModel.PROP_USERNAME, testUserName);
		properties.put(ContentModel.PROP_PASSWORD, testUserName);
		properties.put(ContentModel.PROP_FIRSTNAME, testUserName);
		properties.put(ContentModel.PROP_LASTNAME, testUserName);
		personNode = personService.createPerson(properties);
		permissionService.setPermission(personNode, testUserName,
		        permissionService.getAllPermission(), true);
		authenticationService.createAuthentication(testUserName,
		        testUserName.toCharArray());
	}

	@Test
	public void testSysMove()
	{
		Exception permissionException = null;

		final NodeService nodeService = serviceRegistry.getNodeService();
		RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{
				authenticationComponent.setCurrentUser(testUserName);

				nodeService.moveNode(docNode, targetFolder,
				        ContentModel.ASSOC_CONTAINS, nodeService
				                .getPrimaryParent(docNode).getQName());
				return null;
			}

		};

		try
		{
			serviceRegistry.getTransactionService()
			        .getRetryingTransactionHelper()
			        .doInTransaction(txnWork, false, false);
		} catch (Exception e)
		{
			permissionException = e;
		}
		Assert.assertNotNull(permissionException);
		Assert.assertTrue(permissionException instanceof AccessDeniedException);
		txnWork = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{
				authenticationComponent.setCurrentUser(testUserName);

				sysMover.move(docNode, targetFolder);
				return null;
			}

		};
		serviceRegistry.getTransactionService().getRetryingTransactionHelper()
		        .doInTransaction(txnWork, false, false);

		Assert.assertEquals(targetFolder.getId(),
		        nodeService.getPrimaryParent(docNode).getParentRef().getId());
		Assert.assertEquals(testUserName,
		        authenticationComponent.getCurrentUserName());
	}
}
