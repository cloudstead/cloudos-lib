package cloudos.cslib.compute.meta;

public interface Footprint {

    public Integer getCpus ();
    public Integer getMemory ();
    public CsUsageLevel getNetworkIoLevel ();
    public CsUsageLevel getDiskIoLevel ();

}
