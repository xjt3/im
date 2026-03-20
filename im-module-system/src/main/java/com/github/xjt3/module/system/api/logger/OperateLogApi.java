package com.github.xjt3.module.system.api.logger;

import com.github.xjt3.framework.common.biz.system.logger.OperateLogCommonApi;
import com.github.xjt3.framework.common.pojo.PageResult;
import com.github.xjt3.module.system.api.logger.dto.OperateLogPageReqDTO;
import com.github.xjt3.module.system.api.logger.dto.OperateLogRespDTO;

/**
 * 操作日志 API 接口
 *
 * @author 芋道源码
 */
public interface OperateLogApi extends OperateLogCommonApi {

    /**
     * 获取指定模块的指定数据的操作日志分页
     *
     * @param pageReqDTO 请求
     * @return 操作日志分页
     */
    PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO);

}
