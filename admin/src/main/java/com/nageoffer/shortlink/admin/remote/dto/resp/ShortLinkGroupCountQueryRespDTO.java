package com.nageoffer.shortlink.admin.remote.dto.resp;


import lombok.Data;

/**
 * 短链接数量查询返回实体对象
 */
@Data
public class ShortLinkGroupCountQueryRespDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接数量
     */
    private Integer shortLinkCount;
}
