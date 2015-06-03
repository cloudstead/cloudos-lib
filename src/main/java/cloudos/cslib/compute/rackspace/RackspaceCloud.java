package cloudos.cslib.compute.rackspace;

import cloudos.cslib.compute.jclouds.JcloudBase;

public class RackspaceCloud extends JcloudBase {

    @Override protected String getImage() { return getRegion() + "/" + super.getImage(); }
    @Override protected String getInstanceSize() { return getRegion() + "/" + super.getInstanceSize(); }

}