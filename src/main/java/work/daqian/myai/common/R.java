package work.daqian.myai.common;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import work.daqian.myai.util.JsonStrSerializer;

import static work.daqian.myai.constant.ErrorInfo.Code.FAILED;
import static work.daqian.myai.constant.ErrorInfo.Code.SUCCESS;
import static work.daqian.myai.constant.ErrorInfo.Msg.OK;


@Data
@Schema(description = "通用响应结果")
public class R<T> {
    @Schema(description = "响应码", example = "200")
    private int code;
    @Schema(description = "响应消息", example = "OK")
    private String msg;
    @Schema(description = "响应数据")
    @JsonSerialize(using = JsonStrSerializer.class)
    private T data;
    @Schema(description = "请求ID", example = "123456789")
    private String requestId;

    public static R<Void> ok() {
        return new R<>(SUCCESS, OK, null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, OK, data);
    }

    public static <T> R<T> error(String msg) {
        return new R<>(FAILED, msg, null);
    }

    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        // this.requestId = MDC.get(Constant.REQUEST_ID_HEADER);
    }

    public boolean success(){
        return code == SUCCESS;
    }

    public R<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
