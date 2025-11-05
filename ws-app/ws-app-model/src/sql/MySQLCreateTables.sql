DROP TABLE IF EXISTS surveyresponse; -- Vecchio nome (dall'errore)
DROP TABLE IF EXISTS Response;       -- Nuovo nome (da Response.java)
DROP TABLE IF EXISTS survey;         -- Vecchio nome (dall'errore)
DROP TABLE IF EXISTS Survey;       -- Nuovo nome (da Survey.java)



CREATE TABLE Survey (
    surveyId BIGINT NOT NULL AUTO_INCREMENT,
    question VARCHAR(255) NOT NULL,
    creationDate DATETIME NOT NULL,
    endDate DATETIME NOT NULL,
    canceled BIT NOT NULL,
    positiveResponses BIGINT NOT NULL,
    negativeResponses BIGINT NOT NULL,
    CONSTRAINT SurveyPK PRIMARY KEY (surveyId)
) ENGINE = InnoDB;

CREATE TABLE Response (
    responseId BIGINT NOT NULL AUTO_INCREMENT,
    surveyId BIGINT NOT NULL,
    employeeEmail VARCHAR(255) NOT NULL,
    response BIT NOT NULL,
    responseDate DATETIME NOT NULL,
    CONSTRAINT ResponsePK PRIMARY KEY (responseId),
    CONSTRAINT ResponseSurveyFK FOREIGN KEY (surveyId)
        REFERENCES Survey(surveyId) ON DELETE CASCADE,
    CONSTRAINT EmailSurveyUnique UNIQUE (employeeEmail, surveyId)
) ENGINE = InnoDB;