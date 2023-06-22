import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static com.google.common.base.CharMatcher.ascii;

public class RequestSender {
    final static HttpClient client = HttpClient.newHttpClient();
    final static ObjectMapper objectMapper = new ObjectMapper();
    final static JavaType type = objectMapper.getTypeFactory().
            constructCollectionType(Set.class, PackageInfo.class);
    final static JavaType type2 = objectMapper.getTypeFactory().constructCollectionType(List.class, PackageInfo.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        executeTask("p10", "p9");
    }

    public static int order(char c)
    {
        if (Character.isDigit(c))
            return 0;
        else if (ascii().matches(c))
            return c;
        else if (c == '~')
            return -1;
        else if (c != ' ')
            return c + 256;
        else
            return 0;
    }

    public static int cmpFragment(String A, String B){
         int a_index = 0;
         int b_index = 0;
         int a_length = A.length();
         int b_length = B.length();
         while (a_index != a_length - 1 && b_index != b_length - 1 ) {
             int first_diff = 0;
             while (a_index != a_length - 1 && b_index != b_length - 1 &&
                     (!Character.isDigit(A.charAt(a_index)) || !Character.isDigit(B.charAt(b_index)))) {
                 int vc = order(A.charAt(a_index));
                 int rc = order(B.charAt(b_index));
                 if (vc != rc)
                     return vc - rc;
                 ++a_index;
                 ++b_index;
             }

             while (A.charAt(a_index) == '0')
                 ++a_index;
             while (B.charAt(b_index) == '0')
                 ++b_index;

             while (Character.isDigit(A.charAt(a_index)) && Character.isDigit(B.charAt(b_index))) {
                 if (first_diff == 0)
                     first_diff = A.charAt(a_index) - B.charAt(b_index);
                 ++a_index;
                 ++b_index;
             }
             if (Character.isDigit(A.charAt(a_index)))
                 return 1;
             if (Character.isDigit(B.charAt(b_index))){
                 return -1;
             }
             if (first_diff != 0)
                 return first_diff;
         }

         if (a_index == a_length - 1 && b_index == b_length - 1)
             return 0;
         if (a_index == a_length - 1) {
             if (B.charAt(b_index) == '~') return 1;
             return -1;
         }
         if (b_index == b_length - 1){
             if (A.indexOf(a_index) == '~') return -1;
             return 1;
         }
         return 1;

    }


    public static int doCmpVersion(String A, String B){
        int a_index = A.indexOf(':');
        int b_index = B.indexOf(':');

        int a_index_2 = 0;
        int b_index_2 = 0;

        if (a_index == -1)
            a_index = 0;
        if (b_index == -1)
            b_index = 0;

        if (a_index != 0){
            while (A.charAt(a_index_2) != '0'){
                if (a_index == a_index_2){
                    ++a_index;
                    ++a_index_2;
                }
                ++a_index_2;
            }
        }
        if (b_index != 0){
            while (B.charAt(b_index_2) != '0'){
                if (b_index == b_index_2){
                    ++b_index;
                    ++b_index_2;
                }
                ++b_index_2;
            }
        }

        int Res = cmpFragment(A.substring(a_index_2, a_index), B.substring(b_index_2, b_index));
        if (Res != 0)
            return Res;

        if (a_index != a_index_2)
            a_index++;
        if (b_index != b_index_2)
            b_index++;

        int dlhs = A.indexOf('-');
        int drhs = B.indexOf('-');

        if (dlhs == -1)
            dlhs = A.length();
        if (drhs == -1)
            drhs = B.length();

        Res = cmpFragment(A.substring(a_index, dlhs), B.substring(b_index, drhs));
        if (Res != 0)
            return Res;

        if (dlhs != a_index)
            dlhs++;
        if (drhs != b_index)
            drhs++;

        if (A.charAt(dlhs - 1) == '-' && B.charAt(drhs - 1) == '-')
            return cmpFragment(A.substring(dlhs), B.substring(drhs));
        else if (A.charAt(dlhs - 1) == '-') {
            return cmpFragment(A.substring(dlhs), "0");

        }
        else if (B.charAt(drhs - 1) == '-'){
            return cmpFragment("0", B.substring(drhs));
        }
        else {
            return 0;
        }


    }

    public static boolean checkPackage(PackageInfo taskPackage1, Set<PackageInfo> taskPackageSet){
        Optional<PackageInfo> taskPackageOptional = taskPackageSet.
                stream().
                filter(packageFromList -> packageFromList
                        .getName()
                        .equals(taskPackage1.getName())).
                findFirst();
        if (taskPackageOptional.isEmpty()){
            return true;
        } else{
            return comparePackages(taskPackage1, taskPackageOptional.get());
        }
    }
    public static boolean comparePackages(PackageInfo taskPackage1, PackageInfo taskPackage2 ){
        int res = doCmpVersion(taskPackage1.getVersion(), taskPackage2.getVersion());
        if (res > 0){
            return true;
        } else if (res < 0) {
            return false;
        }
        else {
            return doCmpVersion(taskPackage1.getRelease(), taskPackage2.getRelease()) > 0;
        }
    }

    public static void executeTask(String firstBranch, String secondBranch) throws IOException, InterruptedException {
        var firstPackages = getPackages(firstBranch);
        var secondPackages = getPackages(secondBranch);
        var task1 = firstPackages.stream().filter(taskPackage -> !secondPackages.contains(taskPackage)).toList();
        var task2 = secondPackages.stream().filter(taskPackage -> !firstPackages.contains(taskPackage)).toList();

        var task3 = firstPackages
                .stream()
                .filter(PackageInfo -> checkPackage(PackageInfo, secondPackages)).toList();

        var result = String.format("{\"first_branch\":%s," +
                "\"unique_from_first_branch\":%s," +
                "second_branch:%s," +
                "\"unique_from_second_branch\":%s," +
                "\"newer_from_first_than_in_second\":%s}",
                firstBranch,
                objectMapper.writerWithType(type2).writeValueAsString(task1),
                secondBranch,
                objectMapper.writerWithType(type2).writeValueAsString(task2),
                objectMapper.writerWithType(type2).writeValueAsString(task3)
                );
        System.out.print(result);

    }
    public static Set<PackageInfo> getPackages(String branch) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder().
                uri(URI.create("https://rdb.altlinux.org/api/export/branch_binary_packages/" + branch))
                .build();
        var response =
                client.send(request, HttpResponse.BodyHandlers.ofLines());
        String result = response.body().findFirst().get();
        String substring = result.substring(result.indexOf("["), result.indexOf("]") + 1);
        return objectMapper.readValue(substring, type);
    }
}
