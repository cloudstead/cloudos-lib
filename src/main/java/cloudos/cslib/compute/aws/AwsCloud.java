package cloudos.cslib.compute.aws;

import cloudos.cslib.compute.jclouds.JcloudBase;

public class AwsCloud extends JcloudBase {

    @Override protected String getImage() { return getRegion() + "/" + super.getImage(); }

}
