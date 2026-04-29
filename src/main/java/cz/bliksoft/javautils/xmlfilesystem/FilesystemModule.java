package cz.bliksoft.javautils.xmlfilesystem;

import cz.bliksoft.javautils.modules.ModuleBase;

public class FilesystemModule extends ModuleBase {

	@Override
	public String getModuleName() {
		return "Filesystem";
	}

	@Override
	public int getModuleLoadingOrder() {
		return -10000;
	}

}
