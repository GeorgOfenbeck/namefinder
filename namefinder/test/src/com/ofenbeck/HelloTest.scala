package com.ofenbeck

class MySuite extends munit.FunSuite {
  test("hello") {
    assertEquals(Hello.msg, "Hello World!")
  }
}

