import java.util.Date;

def createSetup(object) {
    object.project_id = project.id;
    object.lock_version = 0;
    object.done_ratio = 0;
    object.author_id = 1;
}

def createValidate(object) {
    Date now = new Date();
    object.created_on = now;
    object.updated_on = now;
}

def editValidate(object) {
    object.updated_on = new Date();
}