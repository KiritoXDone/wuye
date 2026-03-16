package com.wuye.common.api;

import java.util.List;

public record PageResponse<T>(List<T> list, int pageNo, int pageSize, long total) {
}
