package team.rescue.recipe.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.rescue.auth.user.PrincipalDetails;
import team.rescue.common.file.FileService;
import team.rescue.error.exception.ServiceException;
import team.rescue.error.type.ServiceError;
import team.rescue.member.dto.MemberDto.MemberInfoDto;
import team.rescue.member.entity.Member;
import team.rescue.member.repository.MemberRepository;
import team.rescue.recipe.dto.RecipeDto.RecipeCreateDto;
import team.rescue.recipe.dto.RecipeDto.RecipeDetailDto;
import team.rescue.recipe.dto.RecipeDto.RecipeInfoDto;
import team.rescue.recipe.dto.RecipeDto.RecipeUpdateDto;
import team.rescue.recipe.dto.RecipeIngredientDto;
import team.rescue.recipe.dto.RecipeStepDto.RecipeStepCreateDto;
import team.rescue.recipe.dto.RecipeStepDto.RecipeStepInfoDto;
import team.rescue.recipe.entity.Recipe;
import team.rescue.recipe.entity.RecipeIngredient;
import team.rescue.recipe.entity.RecipeStep;
import team.rescue.recipe.repository.RecipeIngredientRepository;
import team.rescue.recipe.repository.RecipeStepRepository;
import team.rescue.recipe.repository.RecipesRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

  private final RecipesRepository recipesRepository;
  private final RecipeIngredientRepository recipeIngredientRepository;
  private final RecipeStepRepository recipeStepRepository;
  private final MemberRepository memberRepository;
  private final FileService fileService;

  public RecipeDetailDto getRecipe(Long id) {

    Recipe recipe = recipesRepository.findById(id)
        .orElseThrow(() -> {
          log.error("레시피 없음");
          return new ServiceException(ServiceError.RECIPE_NOT_FOUND);
        });
    log.debug("레시피 {}", recipe);

    Long memberId = recipe.getMember().getId();
    log.debug("member Id {}", memberId);

    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> {
          log.error("일치하는 사용자 정보 없음");
          return new ServiceException(ServiceError.USER_NOT_FOUND);
        });

    MemberInfoDto memberInfoDto = MemberInfoDto.of(member);

    List<RecipeIngredient> recipeIngredientList =
        recipeIngredientRepository.findByRecipe(recipe);
    List<RecipeIngredientDto> recipeIngredientDtoList =
        recipeIngredientList.stream().map(RecipeIngredientDto::of).toList();

    List<RecipeStep> recipeStepList =
        recipeStepRepository.findByRecipe(recipe);
    List<RecipeStepInfoDto> recipeStepDtoList =
        recipeStepList.stream().map(RecipeStepInfoDto::of).toList();

    return RecipeDetailDto.builder()
        .id(recipe.getId())
        .title(recipe.getTitle())
        .summary(recipe.getSummary())
        .recipeImageUrl(recipe.getRecipeImageUrl())
        .viewCount(recipe.getViewCount())
        .reviewCount(recipe.getReviewCount())
        .reportCount(recipe.getReportCount())
        .bookmarkCount(recipe.getBookmarkCount())
        .createdAt(recipe.getCreatedAt())
        .recipeIngredients(recipeIngredientDtoList)
        .recipeSteps(recipeStepDtoList)
        .author(memberInfoDto)
        .build();
  }

  @Transactional
  public RecipeCreateDto addRecipe(RecipeCreateDto recipeCreateDto,
      PrincipalDetails principalDetails) {

    String memberEmail = principalDetails.getMember().getEmail();

    Member member = memberRepository.findUserByEmail(memberEmail)
        .orElseThrow(() -> {
          log.error("일치하는 사용자 정보 없음");
          return new ServiceException(ServiceError.USER_NOT_FOUND);
        });

    // 레시피 대표 이미지 저장
    String recipeImageFilePath = fileService.uploadImageToS3(recipeCreateDto.getRecipeImageUrl());

    Recipe recipe = Recipe.builder()
        .title(recipeCreateDto.getTitle())
        .summary(recipeCreateDto.getSummary())
        .recipeImageUrl(recipeImageFilePath)
        .viewCount(0)
        .reviewCount(0)
        .reportCount(0)
        .bookmarkCount(0)
        .member(member) // 멤버 연결
        .build();

    recipesRepository.save(recipe); // 먼저 Recipe 저장

    for (RecipeIngredientDto recipeIngredientDto : recipeCreateDto.getRecipeIngredients()) {
      RecipeIngredient ingredient = RecipeIngredient.builder()
          .name(recipeIngredientDto.getName())
          .amount(recipeIngredientDto.getAmount())
          .recipe(recipe) // 재료와 레시피 연결
          .build();
      recipeIngredientRepository.save(ingredient);
    }

    // 레시피 스탭들 저장
    for (RecipeStepCreateDto recipeStepCreateDto : recipeCreateDto.getRecipeSteps()) {

      String stepImageFilePath = "";  // 빈 문자열
      if (!recipeStepCreateDto.getStepImageUrl().isEmpty()) {
        // 스탭 이미지 저장
        stepImageFilePath = fileService.uploadImageToS3(recipeStepCreateDto.getStepImageUrl());
      }

      RecipeStep step = RecipeStep.builder()
          .stepNo(recipeStepCreateDto.getStepNo())
          .stepImageUrl(stepImageFilePath) // URL 설정
          .stepContents(recipeStepCreateDto.getStepContents())
          .stepTip(recipeStepCreateDto.getStepTip())
          .recipe(recipe) // 레시피와 연결
          .build();

      recipeStepRepository.save(step);
    }

    return RecipeCreateDto.of(recipe);
  }


  @Transactional
  public RecipeDetailDto updateRecipe(Long recipeId,
      RecipeUpdateDto recipeUpdateDto,
      PrincipalDetails principalDetails) {

    Long memberId = principalDetails.getMember().getId();

    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> {
          log.error("일치하는 사용자 정보 없음");
          return new ServiceException(ServiceError.USER_NOT_FOUND);
        });

    Recipe recipe = recipesRepository.findById(recipeId)
        .orElseThrow(() -> {
          log.error("레시피 없음");
          return new ServiceException(ServiceError.RECIPE_NOT_FOUND);
        });

    if (!recipe.getMember().equals(member)) {
      log.error("레시피를 작성한 회원이 아님");
      throw new ServiceException(ServiceError.RECIPE_MEMBER_UNMATCHED);
    }

    // 레시피 대표 이미지 업데이트
    fileService.deleteImages(recipe.getRecipeImageUrl());
    String recipeImageFilePath = fileService.uploadImageToS3(recipeUpdateDto.getRecipeImageUrl());

    recipe.update(
            recipeUpdateDto.getTitle(),
            recipeUpdateDto.getSummary(),
            recipeImageFilePath
				);

    recipesRepository.save(recipe);

    // 레시피 ingredient 수정
    List<RecipeIngredient> existingRecipeIngredientList =
        recipeIngredientRepository.findByRecipe(recipe);
    List<RecipeIngredientDto> updatedRecipeIngredients = new ArrayList<>();
    for (RecipeIngredientDto recipeIngredientDto : recipeUpdateDto.getRecipeIngredients()) {

      log.debug("레세피 재료 아이디: {}", recipeIngredientDto.getName());

      // 기존 재료가 있는지 확인
      Optional<RecipeIngredient> existingIngredient = existingRecipeIngredientList.stream()
          .filter(ingredient -> ingredient.getName().equals(recipeIngredientDto.getName()))
          .findFirst();

      if (existingIngredient.isPresent()) {
        // 기존 재료 수정
        RecipeIngredient updatedIngredient = existingIngredient.get();

        updatedIngredient.updateRecipeIngredient(
            recipeIngredientDto.getName(), recipeIngredientDto.getAmount());

        recipeIngredientRepository.save(updatedIngredient);

        updatedRecipeIngredients.add(RecipeIngredientDto.of(updatedIngredient));
      }
      // 새로운 재료라면 추가
      else {
        RecipeIngredient newIngredient = RecipeIngredient.builder()
            .name(recipeIngredientDto.getName())
            .amount(recipeIngredientDto.getAmount())
            .recipe(recipe)
            .build();
        recipeIngredientRepository.save(newIngredient);

        updatedRecipeIngredients.add(RecipeIngredientDto.of(newIngredient));
      }
    }

    // 재료 삭제 처리
    List<RecipeIngredient> ingredientsToDelete = existingRecipeIngredientList.stream()
        .filter(ingredient -> recipeUpdateDto.getRecipeIngredients().stream()
            .noneMatch(dto -> dto.getName().equals(ingredient.getName()))).toList();

    recipeIngredientRepository.deleteAll(ingredientsToDelete);

    // 레시피 step 수정
    List<RecipeStep> existingRecipeStepList = recipeStepRepository.findByRecipe(recipe);
    List<RecipeStepInfoDto> updatedRecipeStep = new ArrayList<>();
    for (RecipeStepCreateDto recipeStepCreateDto : recipeUpdateDto.getRecipeSteps()) {

      Optional<RecipeStep> existingStep = existingRecipeStepList.stream()
          .filter(ingredient -> ingredient.getStepNo() == (recipeStepCreateDto.getStepNo()))
          .findFirst();

      if (existingStep.isPresent()) {
        // 기존 스텝의 이미지 삭제
        fileService.deleteImages(existingStep.get().getStepImageUrl());

        // 새 이미지 파일 경로 얻기
        String newStepImageFilePath = fileService.uploadImageToS3(
            recipeStepCreateDto.getStepImageUrl());

        RecipeStep updatedStep = existingStep.get();

        // 스텝 업데이트
        updatedStep.updateRecipeStep(
            recipeStepCreateDto.getStepNo(),
            newStepImageFilePath,
            recipeStepCreateDto.getStepContents(),
            recipeStepCreateDto.getStepTip()
        );
        recipeStepRepository.save(updatedStep);

        updatedRecipeStep.add(RecipeStepInfoDto.of(updatedStep));
      } else {
        String stepImageFilePath = "";  // 빈 문자열
        if (!recipeStepCreateDto.getStepImageUrl().isEmpty()) {
          // 스탭 이미지 저장
          stepImageFilePath = fileService.uploadImageToS3(recipeStepCreateDto.getStepImageUrl());
        }

        RecipeStep newStep = RecipeStep.builder()
            .stepNo(recipeStepCreateDto.getStepNo())
            .stepImageUrl(stepImageFilePath) // URL 설정
            .stepContents(recipeStepCreateDto.getStepContents())
            .stepTip(recipeStepCreateDto.getStepTip())
            .recipe(recipe) // 레시피와 연결
            .build();

        updatedRecipeStep.add(RecipeStepInfoDto.of(newStep));

        recipeStepRepository.save(newStep);
      }
    }
    // 스탭 삭제 처리
    List<RecipeStep> stepToDelete = existingRecipeStepList.stream()
        .filter(ingredient -> recipeUpdateDto.getRecipeSteps().stream()
            .noneMatch(dto -> dto.getStepNo() == (ingredient.getStepNo()))).toList();

    recipeStepRepository.deleteAll(stepToDelete);

    return RecipeDetailDto.builder()
        .title(recipe.getTitle())
        .summary(recipe.getSummary())
        .recipeImageUrl(recipeImageFilePath)
        .recipeIngredients(updatedRecipeIngredients)
        .recipeSteps(updatedRecipeStep)
        .build();

  }

  @Transactional
  public RecipeInfoDto deleteRecipe(Long recipeId,
      PrincipalDetails principalDetails) {

    Long memberId = principalDetails.getMember().getId();

    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> {
          log.error("일치하는 사용자 정보 없음");
          return new ServiceException(ServiceError.USER_NOT_FOUND);
        });

    Recipe recipe = recipesRepository.findById(recipeId)
        .orElseThrow(() -> {
          log.error("레시피 없음");
          return new ServiceException(ServiceError.RECIPE_NOT_FOUND);
        });

    if (!recipe.getMember().equals(member)) {
      log.error("레시피를 작성한 회원이 아님");
      throw new ServiceException(ServiceError.RECIPE_MEMBER_UNMATCHED);
    }

    // 레시피 대표 이미지 삭제
    fileService.deleteImages(recipe.getRecipeImageUrl());

    // 레시피 ingredient 삭제
    List<RecipeIngredient> existingRecipeIngredientList =
        recipeIngredientRepository.findByRecipe(recipe);
    recipeIngredientRepository.deleteAll(existingRecipeIngredientList);


    // 레시피 step 삭제
    List<RecipeStep> existingRecipeStepList = recipeStepRepository.findByRecipe(recipe);
    for (RecipeStep existingRecipeStep : existingRecipeStepList) {

      // 이미지가 있는 스텝이면 s3에서 삭제
      if (!(existingRecipeStep.getStepImageUrl() == null || existingRecipeStep.getStepImageUrl().isEmpty())) {
        fileService.deleteImages(existingRecipeStep.getStepImageUrl());
      }
    }
    recipeStepRepository.deleteAll(existingRecipeStepList);

    recipesRepository.delete(recipe);

    return RecipeInfoDto.of(recipe);
  }
}
