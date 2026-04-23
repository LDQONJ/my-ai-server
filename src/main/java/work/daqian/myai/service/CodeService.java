package work.daqian.myai.service;

import work.daqian.myai.common.R;

public interface CodeService {
    R<Void> sendCode(String target);
}
