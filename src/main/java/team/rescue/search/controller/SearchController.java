package team.rescue.search.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.rescue.common.dto.ResponseDto;
import team.rescue.recipe.dto.RecipeDto.RecipeInfoDto;
import team.rescue.search.service.RecipeSearchService;
import team.rescue.search.type.SortType;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

	private final RecipeSearchService recipeSearchService;

	@GetMapping("/recipe/keyword")
	public ResponseEntity<ResponseDto<SearchPage<RecipeInfoDto>>> searchRecipesByKeyword(
			@RequestParam String keyword,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "createdAt") SortType sortType
	) {

		log.info("[레시피 키워드 검색] keyword={}, sortType={}", keyword, sortType.name());

		PageRequest pageRequest = PageRequest.of(
				page, 10, Sort.by(Direction.DESC, sortType.getSortBy())
		);

		SearchPage<RecipeInfoDto> recipeInfoList =
				recipeSearchService.searchRecipeByKeyword(keyword, pageRequest);

		return new ResponseEntity<>(
				new ResponseDto<>(keyword + "로 검색한 레시피 목록입니다.", recipeInfoList),
				HttpStatus.OK
		);
	}
  private final SearchService searchService;

  /**
   *
   */
  @GetMapping("/ingredient")
  public List<String> getIngredient(
      @RequestParam String keyword
  ) {

    List<String> ingredients = searchService.getIngredient(keyword);

    return ingredients;
  }

}
