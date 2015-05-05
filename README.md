Generically meant to be used in content rules where documents may need to be moved to folders where the current user may not have (write) access.

It defines an extension JS variable called sysMover that can be called for moving a document to any folder in the repository (the move is executed as system user).

There is an example of a javascript using the extension here: 
https://github.com/rjmfernandes/sys-move-alfresco-js-ext/blob/master/sample/moveTo_sample1.js
This particular sample script expects the user to have access at least as readonly mode to the final folder the document will be moved to (the script fetches the folder by path to be used as target on the method call). 
If you wanted a move to a folder where even read may not be available to user, you will probably need to have another extension method that fetches folders by path independently of the user permissions, or just tweak the present extension to always make the move to the same target folder(with no need of receiving as argument).

This is a maven project and as such can be compiled to generate the corresponding amp to extend the alfresco repository webapp. For making the life easier to some the final amp is also available here: 
https://github.com/rjmfernandes/sys-move-alfresco-js-ext/blob/master/dist/sysMoveJSExt.amp
