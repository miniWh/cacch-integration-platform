package com.cacch.integration.integration.oa.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OaRegItemRequiredSupport} 单元测试
 */
class OaRegItemRequiredSupportTest {

    @Test
    void isRequired_blankOrZero_treatedAsRequired() {
        assertTrue(OaRegItemRequiredSupport.isRequired(null));
        assertTrue(OaRegItemRequiredSupport.isRequired(""));
        assertTrue(OaRegItemRequiredSupport.isRequired("0"));
        assertTrue(OaRegItemRequiredSupport.isRequired("需要"));
    }

    @Test
    void isRequired_one_treatedAsNotRequired() {
        assertFalse(OaRegItemRequiredSupport.isRequired("1"));
    }

    @Test
    void isExplicitlyNotRequired_onlyOne() {
        assertTrue(OaRegItemRequiredSupport.isExplicitlyNotRequired("1"));
        assertFalse(OaRegItemRequiredSupport.isExplicitlyNotRequired("0"));
        assertFalse(OaRegItemRequiredSupport.isExplicitlyNotRequired(null));
    }
}
