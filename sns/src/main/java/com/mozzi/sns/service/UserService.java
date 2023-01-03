package com.mozzi.sns.service;


import com.mozzi.sns.domain.User;
import com.mozzi.sns.domain.entity.UserEntity;
import com.mozzi.sns.exception.ErrorCode;
import com.mozzi.sns.exception.GlobalException;
import com.mozzi.sns.repository.UserEntityRepository;
import com.mozzi.sns.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expired-time-ms}")
    private Long expiredTimes;


    // 유저체크용
    public User loadUserByUserName(String userName) {
        return userEntityRepository.findByUserName(userName).map(User::fromEntity).orElseThrow(() ->
                new GlobalException(ErrorCode.USER_NOT_FOUND, String.format("%s not found", userName)));
    }

    // 회원가입
    @Transactional
    public User join(String userName, String password) {

        // 회원가입 체크
        userEntityRepository.findByUserName(userName).ifPresent(it -> {
            throw new GlobalException(ErrorCode.DUPLICATED_USER_NAME, String.format("%s is duplicated", userName));
        });

        // 회원가입 진행
        UserEntity userEntity = userEntityRepository.save(UserEntity.of(userName, bCryptPasswordEncoder.encode(password)));
        return User.fromEntity(userEntity);
    }


    // 로그인
    public String login(String userName, String password) {

        // 회원가입 체크
        UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)));

        // 비밀번호  체크
        if(!bCryptPasswordEncoder.matches(password, userEntity.getPassword())){
            throw new GlobalException(ErrorCode.INVALID_PASSWORD);
        }
        // 토큰생성
        return JwtTokenUtils.generateToken(userName, secretKey, expiredTimes);
    }
}
