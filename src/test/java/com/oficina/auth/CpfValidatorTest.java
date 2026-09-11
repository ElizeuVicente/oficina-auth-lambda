package com.oficina.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CpfValidatorTest {
    @Test void acceptsValidCpf() { assertTrue(CpfValidator.isValid("529.982.247-25")); }
    @Test void rejectsInvalidCpf() { assertFalse(CpfValidator.isValid("111.111.111-11")); }
}
