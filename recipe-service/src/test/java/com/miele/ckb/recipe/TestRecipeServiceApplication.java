package com.miele.ckb.recipe;

import org.springframework.boot.SpringApplication;

public class TestRecipeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(RecipeServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
