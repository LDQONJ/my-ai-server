package work.daqian.myai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import work.daqian.myai.common.R;
import work.daqian.myai.service.UpdateService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
public class UpdateController {

    private final UpdateService updateService;

    @PostMapping("/check")
    public R<String> checkUpdate(@RequestBody Map<String, String> versionMap) {
        return updateService.checkUpdate(versionMap);
    }

    @GetMapping("/latestRelease")
    public ResponseEntity<Resource> download() {
        return updateService.download();
    }
}
