import lombok.*;


@Data
public class PackageInfo {
    private String name;
    private int epoch;
    private String version;
    private String release;
    private String arch;
    private String disttag;
    private int buildtime;
    private String source;
}
