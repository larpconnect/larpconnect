package com.larpconnect.njall.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CalculatorTest {

  @Test
  void should_addTwoNumbers() {
    Calculator calculator = new Calculator();
    assertEquals(5, calculator.add(2, 3));
  }
}
