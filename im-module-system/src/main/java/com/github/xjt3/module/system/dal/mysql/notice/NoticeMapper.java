package com.github.xjt3.module.system.dal.mysql.notice;

import com.github.xjt3.framework.common.pojo.PageResult;
import com.github.xjt3.framework.mybatis.core.mapper.BaseMapperX;
import com.github.xjt3.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.github.xjt3.module.system.controller.admin.notice.vo.NoticePageReqVO;
import com.github.xjt3.module.system.dal.dataobject.notice.NoticeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapperX<NoticeDO> {

    default PageResult<NoticeDO> selectPage(NoticePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NoticeDO>()
                .likeIfPresent(NoticeDO::getTitle, reqVO.getTitle())
                .eqIfPresent(NoticeDO::getStatus, reqVO.getStatus())
                .orderByDesc(NoticeDO::getId));
    }

}
