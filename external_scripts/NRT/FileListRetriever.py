import sqlite3
import time
from abc import abstractmethod
from hashlib import sha256

from DataRetriever import DataRetriever
from NotFoundException import NotFoundException

STATUS_COMPLETE = 1
STATUS_RETRY = 0
STATUS_FAILED = -1


def timestamp():
    return int(time.time())


class FileListRetriever(DataRetriever):
    _DB_FILE = 'FileListRetriever.sqlite'

    def __init__(self, instrument_id, logger, configuration=None):
        super().__init__(instrument_id, logger, configuration)

        # Check that the database exists
        if not self._is_db_set_up():
            self._init_db()

        # Tracker for retrieved files
        self._file_list = None
        self._current_file_index = -1

    def _is_db_set_up(self):
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='files'")
            result = len(cursor.fetchall()) > 0
        return result

    def _init_db(self):
        # Create the files table
        table_sql = ("CREATE TABLE files("
                     "instrument_id INTEGER, "
                     "filename TEXT, "
                     "hashsum TEXT, "
                     "status INTEGER, "
                     "timestamp INTEGER, "
                     "PRIMARY KEY (instrument_id, filename)"
                     ")"
                     )

        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute(table_sql)
            conn.commit()

    # Get the configuration type
    @staticmethod
    @abstractmethod
    def get_type():
        raise NotImplementedError("get_type not implemented")

    # Test the configuration to make sure everything works
    @abstractmethod
    def test_configuration(self):
        raise NotImplementedError("test_configuration not implemented")

    # Initialise the retriever ready to retrieve files
    @abstractmethod
    def startup(self):
        raise NotImplementedError("startup not implemented")

    # Clean up the retriever
    @abstractmethod
    def shutdown(self):
        raise NotImplementedError("shutdown not implemented")

    # Get the next file to be processed
    # and put it in the current_files variable in the form:
    # [{name="xx", content=<bytes>}]
    #
    # We only download one file at a time
    def _retrieve_next_file_set(self):

        if self._file_list is None:
            self._file_list = self._get_all_files()

        # Loop through all files until we find one that needs processing
        file_to_process = None

        while file_to_process is None and self._current_file_index < len(self._file_list) - 1:
            self._current_file_index += 1
            filename = self._file_list[self._current_file_index]

            process_file = False

            if self._needs_processing(filename):
                process_file = True
            else:
                if self._file_updated(filename, self._get_hashsum(filename)):
                    process_file = True

            if process_file:
                file_to_process = filename
                break

        if file_to_process is not None:
            self._add_file(file_to_process, self._get_file_content(file_to_process))

    def _record_file(self, status):
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()

            # Update database (add or update)
            for file in self.current_files:
                if not self._file_known(file["filename"]):
                    sql = ("INSERT INTO files "
                           "(instrument_id, filename, hashsum, status, timestamp) "
                           "VALUES (?, ?, ?, ?, ?)"
                           )

                    cursor.execute(sql,
                                   (self.instrument_id, file["filename"], self._hashsum(file["contents"]),
                                    status, timestamp()))

                else:
                    sql = ("UPDATE files SET "
                           "hashsum = ?, status = ?, timestamp = ? "
                           "WHERE instrument_id = ? AND filename = ?"
                           )

                    cursor.execute(sql,
                                   (self._hashsum(file["contents"]), status, timestamp(),
                                    self.instrument_id, file["filename"]))

            conn.commit()

    # Record the successful processing of the current files
    def _cleanup_success(self):
        self._record_file(STATUS_COMPLETE)

    # The file(s) were not processed successfully;
    # clean them up accordingly
    def _cleanup_fail(self):
        self._record_file(STATUS_FAILED)

    # The file(s) were not processed this time;
    # clean them up so they can be reprocessed later
    def _cleanup_not_processed(self):
        self._record_file(STATUS_RETRY)

    # Get all the files from the source
    @abstractmethod
    def _get_all_files(self):
        raise NotImplementedError("_get_all_files not implemented")

    # Get the hashsum for a file
    @abstractmethod
    def _get_hashsum(self, filename):
        raise NotImplementedError("_get_hashsum not implemented")

    # Get the hashsum for a file
    @abstractmethod
    def _get_file_content(self, filename):
        raise NotImplementedError("_get_file_content not implemented")

    # By default we don't do any cleanup.
    # Concrete implementations can override if they wish
    def _cleanup_file_action(self, filename):
        pass

    # See if a given file needs processing
    def _needs_processing(self, filename):
        result = False

        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT filename, status FROM files WHERE instrument_id = ? AND filename = ?",
                           (self.instrument_id, filename))

            record = cursor.fetchone()
            if record is None:
                result = True
            elif record[1] == STATUS_RETRY:
                result = True

        return result

    # See if a file is known in the database
    def _file_known(self, filename):
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT filename FROM files WHERE instrument_id = ? AND filename = ?",
                           (self.instrument_id, filename))
            result = len(cursor.fetchall()) > 0
        return result

    # See if the hashsum for a file has changed
    def _file_updated(self, filename, new_hashsum):
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT hashsum FROM files WHERE instrument_id = ? AND filename = ?",
                           (self.instrument_id, filename))
            row = cursor.fetchone()
            if row is None:
                raise NotFoundException('Database entry', filename)
            else:
                old_hashsum = row[0]
                result = new_hashsum != old_hashsum

        return result

    @staticmethod
    def _hashsum(data):
        sha_input = data if type(data) == bytearray else data.encode('utf-8')
        return sha256(sha_input).hexdigest()