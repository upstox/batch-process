package com.batch.process.common.utils.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.batch.process.common.validation.model.Problem;

@Component
public class GenericValidationExecutor {

    @Autowired
    private Validator validator;

    public List<Problem> validate(Object o) {
        final List<Problem> problems = new ArrayList<>();

        if (o != null) {
            final Set<ConstraintViolation<Object>> constraintViolations = validator.validate(o);

            if (!constraintViolations.isEmpty()) {

                for (ConstraintViolation<Object> constraintViolation : constraintViolations) {
                    Problem problem = new Problem(constraintViolation.getMessage(),
                            constraintViolation.getPropertyPath()
                                    .toString());
                    problems.add(problem);
                }
            }
        }
        return problems;
    }
}
