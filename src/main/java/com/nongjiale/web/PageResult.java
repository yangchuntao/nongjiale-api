package com.nongjiale.web;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {}
