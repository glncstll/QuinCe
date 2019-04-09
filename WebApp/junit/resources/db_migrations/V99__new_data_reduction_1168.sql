-- Changes for the new data reduction scheme
CREATE TABLE measurements (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  dataset_id INT NOT NULL,
  variable_id INT NOT NULL,
  date BIGINT(20) NOT NULL,
  run_type VARCHAR(45) NULL,
  PRIMARY KEY (id),
  CONSTRAINT measurement_dataset
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

CREATE INDEX measurement_dataset_idx ON measurements(`dataset_id` ASC);