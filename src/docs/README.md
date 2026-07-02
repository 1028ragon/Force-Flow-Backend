# Git Convention

이 문서는 팀 프로젝트에서 Git을 사용할 때 지켜야 할 브랜치, 커밋, PR 규칙을 정리한 문서입니다.

---

## 1. 브랜치 전략

### 기본 브랜치

| 브랜치명 | 설명 |
|---|---|
| `main` | 최종 배포용 브랜치 |
| `develop` | 개발 통합 브랜치 |
| `feature/*` | 기능 개발 브랜치 |
| `fix/*` | 버그 수정 브랜치 |
| `hotfix/*` | 긴급 수정 브랜치 |
| `refactor/*` | 리팩토링 브랜치 |

---

## 2. 브랜치 이름 규칙

브랜치명은 아래 형식을 사용합니다.

```bash
타입/작업내용
```

### 예시

```bash
feature/login
feature/post-crud
feature/jwt-auth
fix/login-error
refactor/post-service
```

---

## 3. 커밋 메시지 규칙

커밋 메시지는 아래 형식을 사용합니다.

```bash
타입: 작업 내용
```

### 커밋 타입

| 타입 | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷팅, 세미콜론 등 기능 변경 없는 수정 |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가 또는 수정 |
| `chore` | 빌드, 설정 파일, 패키지 수정 |
| `rename` | 파일명 또는 폴더명 변경 |
| `remove` | 파일 삭제 |

### 커밋 예시

```bash
feat: 회원가입 기능 추가
feat: JWT 로그인 기능 구현
fix: 로그인 실패 시 403 오류 수정
docs: README 깃 컨벤션 추가
refactor: 게시글 서비스 로직 정리
chore: application.yml 설정 수정
```

---

## 4. 커밋 작성 규칙

- 커밋 메시지는 한글 또는 영어로 작성할 수 있습니다.
- 한 커밋에는 하나의 작업만 포함합니다.
- 의미 없는 커밋 메시지는 사용하지 않습니다.

### 좋지 않은 예시

```bash
수정
업데이트
버그 고침
작업함
```

### 좋은 예시

```bash
fix: 회원가입 중복 아이디 예외 처리 추가
feat: 게시글 삭제 API 구현
docs: API 명세 문서 추가
```

---

## 5. 작업 흐름

### 1. 최신 develop 브랜치 가져오기

```bash
git checkout develop
git pull origin develop
```

### 2. 기능 브랜치 생성

```bash
git checkout -b feature/작업내용
```

예시:

```bash
git checkout -b feature/jwt-auth
```

### 3. 작업 후 커밋

```bash
git add .
git commit -m "feat: JWT 인증 기능 추가"
```

### 4. 원격 저장소에 브랜치 올리기

```bash
git push origin feature/jwt-auth
```

### 5. Pull Request 생성

GitHub에서 `feature/*` 브랜치를 `develop` 브랜치로 Pull Request를 생성합니다.

---

## 6. Pull Request 규칙

PR 제목은 아래 형식을 사용합니다.

```bash
[타입] 작업 내용
```

### PR 제목 예시

```bash
[feat] JWT 인증 기능 구현
[fix] 로그인 오류 수정
[docs] README 수정
```

### PR 내용에 포함할 것

```markdown
## 작업 내용
- 

## 변경 사항
- 

## 테스트 내용
- 

## 참고 사항
-
```

---

## 7. Merge 규칙

- `main` 브랜치에 직접 push하지 않습니다.
- 기능 개발은 반드시 `feature/*` 브랜치에서 진행합니다.
- PR은 팀원 리뷰 후 merge합니다.
- 충돌이 발생하면 작업자가 직접 해결합니다.

---

## 8. 협업 시 주의사항

- 작업 시작 전 항상 `develop` 브랜치를 최신 상태로 맞춥니다.
- 같은 파일을 동시에 수정할 경우 미리 팀원과 공유합니다.
- 큰 기능은 작은 단위로 나누어 커밋합니다.
- 커밋 메시지만 봐도 어떤 작업을 했는지 알 수 있게 작성합니다.

---

## 9. 자주 사용하는 명령어

```bash
# 현재 브랜치 확인
git branch

# 변경 파일 확인
git status

# 브랜치 이동
git checkout 브랜치명

# 새 브랜치 생성 및 이동
git checkout -b 브랜치명

# 변경 파일 스테이징
git add .

# 커밋
git commit -m "타입: 작업 내용"

# 원격 저장소에 push
git push origin 브랜치명

# 최신 코드 가져오기
git pull origin develop
```

---

## 10. 예시 작업 시나리오

```bash
git checkout develop
git pull origin develop
git checkout -b feature/post-crud

# 코드 작업 후
git add .
git commit -m "feat: 게시글 CRUD 기능 구현"
git push origin feature/post-crud
```

이후 GitHub에서 Pull Request를 생성합니다.
