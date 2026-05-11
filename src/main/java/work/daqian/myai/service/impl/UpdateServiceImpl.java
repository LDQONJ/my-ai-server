package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.R;
import work.daqian.myai.exception.BizIllegalException;
import work.daqian.myai.service.UpdateService;

import java.io.File;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService, InitializingBean {

    @Override
    public R<String> checkUpdate(Map<String, String> versionMap) {
        String versionName = versionMap.get("versionName");
        if (versionName != null && latestVersionName != null
        && !latestVersionName.equals(versionName))
            return R.ok("yes");
        try {
            latestVersionName = getLatestVersionName();
        } catch (Exception e) {
            throw new BizIllegalException("读取最新版本失败");
        }
        boolean needUpdate = !latestVersionName.equals(versionName);
        return R.ok(needUpdate ? "yes" : "no");
    }

    @Override
    public ResponseEntity<Resource> download() {
        String apk = apkDir + apkName;
        File apkFile = new File(apk);
        if (!apkFile.exists()) {
            try {
                getLatestVersionName();
            } catch (Exception e) {
                return null;
            }
            apkFile = new File(apkDir + apkName);
        }
        FileSystemResource resource = new FileSystemResource(apkFile);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + apkFile.getName() + "\"")
                .body(resource);
    }

    @Value("${apk.dir}")
    private String apkDir;

    private volatile static String latestVersionName;

    private volatile static String apkName;

    /**
     * 读取单个 APK 的版本信息
     */
    private String getLatestVersionName() throws Exception {
        File apkDir = new File(this.apkDir);
        File[] files = apkDir.listFiles();
        String latestVersionName = null;
        for (File file : files) {
            String fileName = file.getName();
            String apkNamePrefix = "LDQ";
            if (!fileName.startsWith(apkNamePrefix))
                continue;
            apkName = fileName;
            String[] split = fileName.split("-");
            for (String s : split) {
                if (s.startsWith("v"))
                    latestVersionName = s;
            }
        }
        return latestVersionName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        latestVersionName = getLatestVersionName();
    }
}


