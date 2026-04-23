package work.daqian.myai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import work.daqian.myai.domain.po.SystemPrompt;
import work.daqian.myai.repository.SystemPromptRepository;
import work.daqian.myai.service.SystemPromptService;

@Service
@RequiredArgsConstructor
public class SystemPromptServiceImpl implements SystemPromptService {

    private final SystemPromptRepository promptRepository;

    @Override
    public void saveOrUpdate(SystemPrompt systemPrompt) {
        SystemPrompt prompt;
        if (systemPrompt.getUserId() != null) {
            prompt = promptRepository.findSystemPromptByUserIdAndType(systemPrompt.getUserId(), systemPrompt.getType());
        } else {
            prompt = promptRepository.findSystemPromptBySessionIdAndType(systemPrompt.getSessionId(), systemPrompt.getType());
        }
        if (prompt == null) {
            promptRepository.save(systemPrompt);
        } else {
            prompt.setContent(systemPrompt.getContent());
            promptRepository.save(prompt);
        }
    }
}
