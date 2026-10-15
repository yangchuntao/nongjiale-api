package com.suzhong.njl.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.Store;

public interface StoreMapper extends BaseMapper<Store> {

    List<Store> searchOnShelf(StoreSearch query);

    long countOnShelf(StoreSearch query);
}
