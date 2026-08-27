# Developing and Registering Tools

1. Create a [resource type](resources.md) if needed
1. Register the [resource type](resources.md) with the program using the resource manager
	1. program.getAssetManager().registerAssetType( AssetType )
1. Create a tool class
1. Register the tool with a resource type using the tool manager
	1. Create the tool metadata object
	1. program.getToolManager().registerTool( resourceType, toolRegistration )

