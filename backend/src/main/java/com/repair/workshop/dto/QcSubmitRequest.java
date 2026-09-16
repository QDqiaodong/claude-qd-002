package com.repair.workshop.dto;

import java.util.List;

/** 质检员一次提交整张项表：每项记过或不过，缺项 / 空结论一律不收。 */
public class QcSubmitRequest {

    public List<ItemResult> items;

    public static class ItemResult {
        public String item;
        /** 过 / 不过 */
        public String result;
        public String remark;
    }
}
