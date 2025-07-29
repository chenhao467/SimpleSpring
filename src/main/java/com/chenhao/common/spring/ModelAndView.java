package com.chenhao.common.spring;

import lombok.Data;

/*
*功能：
 作者：chenhao
*日期： 2025/4/27 下午10:12
*/
@Data
public class ModelAndView {
    private String view;

    public ModelAndView(String view) {
        this.view = view;
    }
}
