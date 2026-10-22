package com.nageoffer.shortlink.project.service.Impl;

import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.service.UrlTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlTitleServiceImpl implements UrlTitleService {
    private final FaviconServiceImpl faviconService;

    @Override
    public String getTitleByUrl(String url) {
        try { return faviconService.title(url); }
        catch (IllegalArgumentException ex) { throw new ClientException(ex.getMessage()); }
    }
}
