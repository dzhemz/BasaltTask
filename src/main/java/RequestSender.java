import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;


public class RequestSender {
    final static HttpClient client = HttpClient.newHttpClient();
    final static ObjectMapper objectMapper = new ObjectMapper();
    final static JavaType type = objectMapper.getTypeFactory().
            constructCollectionType(Set.class, PackageInfo.class);
    final static JavaType type2 = objectMapper.getTypeFactory().constructCollectionType(List.class, PackageInfo.class);

    public static int order(char c)
    {
        if (Character.isDigit(c))
            return c;
        else if (Character.isAlphabetic(c))
            return c;
        else if (c == '~')
            return -1;
        else if (c != ' ')
            return c + 256;
        else
            return 0;
    }

    public static int cmpFragment(String first, String second) {

        var firstSize = first.length();
        var secondSize = second.length();

        for (int i = 0; i < Integer.min(firstSize, secondSize); i++){
            if (order(first.charAt(i)) - order(second.charAt(i)) > 0){
                return 1;
            } else if (order(first.charAt(i)) - order(second.charAt(i)) < 0) {
                return -1;
            }
        }
        return firstSize - secondSize;
    }

    public static boolean checkPackage(PackageInfo taskPackage1, Map<String, PackageInfo> taskPackageMap){
        PackageInfo taskPackage2 = taskPackageMap.get(taskPackage1.getName());
        if (taskPackage2 == null){
            return true;
        } else {
            int res = cmpFragment(taskPackage1.getVersion(), taskPackage2.getVersion());
            if (res > 0){
                return true;
            } else if (res < 0) {
                return false;
            }
            else {
                return cmpFragment(taskPackage1.getRelease(), taskPackage2.getRelease()) > 0;
            }
        }
    }

    public static void executeTask(String firstBranch, String secondBranch) throws IOException, InterruptedException {
        var firstPackages = getPackages(firstBranch);
        var secondPackages = getPackages(secondBranch);

        HashMap<String, PackageInfo> secondMap = new HashMap<>();
        assert secondPackages != null;
        secondPackages.forEach(elem -> secondMap.put(elem.getName(), elem));

        assert firstPackages != null;
        var task1 = firstPackages.stream().filter(taskPackage -> !secondPackages.contains(taskPackage)).toList();
        var task2 = secondPackages.stream().filter(taskPackage -> !firstPackages.contains(taskPackage)).toList();
        var task3 = firstPackages
                .stream()
                .filter(PackageInfo -> checkPackage(PackageInfo, secondMap)).toList();


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
        Optional<String> result = response.body().findFirst();
        if (result.isPresent()) {
            var resultString = result.get();
            String substring = resultString.substring(resultString.indexOf("["), resultString.indexOf("]") + 1);
            return objectMapper.readValue(substring, type);
        }
        return null;
    }
}
