import {
  AfterViewInit,
  Component,
  OnInit,
  ViewChild
} from "@angular/core";
import {PortofinoService} from "../portofino.service";
import {HttpClient} from "@angular/common/http";
import {Page, PageService} from "../page";
import {map} from "rxjs/operators";
import {ActivatedRoute, Router} from "@angular/router";
import {AuthenticationService} from "../security/authentication.service";
import {Field, Form, FormComponent} from "../form";
import {ClassAccessor, Property} from "../class-accessor";
import {Button} from "../buttons";
import {NotificationService} from "../notifications/notification.service";
import {TranslateService} from "@ngx-translate/core";
import {BehaviorSubject, merge, Observable} from "rxjs";
import {FlatTreeControl} from "@angular/cdk/tree";
import {CollectionViewer, SelectionChange} from "@angular/cdk/collections";
import {FormGroup} from "@angular/forms";

@Component({
  selector: 'portofino-upstairs',
  templateUrl: './upstairs.component.html'
})
export class UpstairsComponent extends Page implements OnInit, AfterViewInit {

  readonly settingsForm = new Form([
    Field.fromProperty(Property.create({name: "appName", label: "Application Name"}).required()),
    Field.fromProperty(Property.create({name: "loginPath", label: "Login Path"}).required())
  ]);
  @ViewChild("settingsFormComponent")
  settingsFormComponent: FormComponent;

  connectionProviders: ConnectionProviderSummary[];
  connectionProvider: ConnectionProviderDetails;
  isEditConnectionProvider = false;
  databasePlatforms: DatabasePlatform[];

  tableTreeControl: FlatTreeControl<TableFlatNode>;
  tableTreeDataSource: TableTreeDataSource;
  tableInfo: any;
  column: any;
  annotationsForm: Form;
  readonly annotations = new FormGroup({});

  wizard: any = { newConnectionType: 'jdbc' };

  constructor(portofino: PortofinoService, http: HttpClient, router: Router, route: ActivatedRoute,
              authenticationService: AuthenticationService, protected pageService: PageService,
              protected notificationService: NotificationService, protected translate: TranslateService) {
    super(portofino, http, router, route, authenticationService);
    this.configuration = { title: "Upstairs" };
    route.url.pipe(map(segments => segments.join(''))).subscribe(url => {
      this.url = url;
    });
    this.tableTreeControl = new FlatTreeControl<TableFlatNode>(this._getLevel, this._isExpandable);
    this.tableTreeDataSource = new TableTreeDataSource(this.tableTreeControl, http, portofino.apiRoot);
  }

  private _getLevel = (node: TableFlatNode) => node.level;

  private _isExpandable = (node: TableFlatNode) => { return node.expandable };

  isExpandable = (_: number, node: TableFlatNode) => { return this._isExpandable(node); };

  ngOnInit(): void {
    this.pageService.notifyPageLoaded(this);
    this.settingsPanel.loadPermissions();
    this.loadConnectionProviders();
    this.loadDatabasePlatforms();
  }

  ngAfterViewInit(): void {
    this.resetSettings();
  }

  loadConnectionProviders() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/connections`;
    this.page.http.get<ConnectionProviderSummary[]>(url).subscribe(s => {
      this.connectionProviders = s;
      this.tableTreeDataSource.data = s.map(c => new TableFlatNode(c.name, null, null));
    });
  }

  openConnectionProvider(conn: ConnectionProviderSummary) {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/connections/${conn.name}`;
    this.page.http.get<ConnectionProviderDetails>(url).subscribe(c => { this.connectionProvider = c; });
  }

  @Button({ list: "connection", text: "Edit", color: "primary", presentIf: UpstairsComponent.isViewConnectionProvider })
  editConnectionProvider() {
    this.isEditConnectionProvider = true;
  }

  static isEditConnectionProvider(self: UpstairsComponent) {
    return self.isEditConnectionProvider;
  }

  static isViewConnectionProvider(self: UpstairsComponent) {
    return !self.isEditConnectionProvider;
  }

  @Button({ list: "connection", text: "Save", color: "primary", icon: "save", presentIf: UpstairsComponent.isEditConnectionProvider })
  saveConnectionProvider() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/connections/${this.connectionProvider.databaseName.value}`;
    this.page.http.put(url, this.connectionProvider).subscribe(() => {
      this.isEditConnectionProvider = false;
    });
  }

  @Button({ list: "connection", text: "Delete", color: "warn", icon: "delete", presentIf: UpstairsComponent.isViewConnectionProvider })
  deleteConnectionProvider() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/connections/${this.connectionProvider.databaseName.value}`;
    this.page.http.delete(url).subscribe(() => {
      this.isEditConnectionProvider = false;
      this.connectionProviders = this.connectionProviders.filter(
        c => c.name != this.connectionProvider.databaseName.value);
      this.connectionProvider = null;
    });
  }

  @Button({ list: "connection", text: "Test", icon: "flash_on", presentIf: UpstairsComponent.isViewConnectionProvider })
  testConnectionProvider() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/connections/${this.connectionProvider.databaseName.value}/:test`;
    this.page.http.post<string[]>(url, {}).subscribe(status => {
      if(status[0] == 'connected') {
        this.notificationService.info(this.translate.instant("Connection tested successfully"));
      }
    });
  }

  @Button({ list: "connection", text: "Synchronize", icon: "refresh", presentIf: UpstairsComponent.isViewConnectionProvider })
  synchronizeConnectionProvider() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/connections/${this.connectionProvider.databaseName.value}/:synchronize`;
    this.page.http.post(url, {}).subscribe(() => {});
  }

  @Button({ list: "connection", text: "Cancel" })
  closeConnectionProvider() {
    this.isEditConnectionProvider = false;
    this.connectionProvider = null;
  }

  loadDatabasePlatforms() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/platforms`;
    this.page.http.get<{ [name: string]: DatabasePlatform }>(url).subscribe(d => {
      this.databasePlatforms = [];
      for (let k in d) {
        this.databasePlatforms.push(d[k]);
      }
    });
  }

  @Button({ list: "settings", text: "Save", color: "primary" })
  saveSettings() {
    this.settingsFormComponent.controls.updateValueAndValidity(); //TODO why is this needed?
    this.http.put(this.portofino.apiRoot + "portofino-upstairs/settings", this.settingsFormComponent.controls.value).subscribe();
  }

  @Button({ list: "settings", text: "Cancel" })
  resetSettings() {
    this.http.get<any>(this.portofino.apiRoot + "portofino-upstairs/settings").subscribe(settings => {
      this.settingsFormComponent.controls.get('appName').setValue(settings.appName.value);
      this.settingsFormComponent.controls.get('loginPath').setValue(settings.loginPath.value);
    });
  }

  editTable(table: TableFlatNode) {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/tables/${table.db}/${table.schema.name}/${table.table}`;
    this.http.get(url).subscribe(tableInfo => {
      this.tableInfo = tableInfo;
      this.tableInfo.db = table.db;
      this.tableInfo.schema = table.schema.name;
      this.prepareTableInfo();
    });
  }

  private prepareTableInfo() {
    this.tableInfo.table.column.forEach(c => {
      if (!c.javaType) {
        c.javaType = "default";
      }
    });
  }

  @Button({ list: "table", text: "Save", icon: "save", color: "primary" })
  saveTable() {
    const table = this.tableInfo.table;
    table.column.forEach(c => {
      if(c.javaType == "default") {
        c.javaType = null;
      }
    });
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/tables/${this.tableInfo.db}/${this.tableInfo.schema}/${table.tableName}`;
    this.http.put(url, this.tableInfo.table).subscribe(
      () => {
        this.prepareTableInfo();
        this.notificationService.info(this.translate.instant("Table saved"));
        }, () => { this.prepareTableInfo(); });
  }

  @Button({ list: "table", text: "Cancel" })
  cancelTable() {
    this.tableInfo = null;
  }

  editColumn(column, index) {
    this.column = column;
    this.column.index = index;
    this.changeType(this.column, this.column.javaType);
  }

  @Button({ list: "column", text: "Save", icon: "save", color: "primary" })
  saveColumn() {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/tables/${this.tableInfo.db}/${this.tableInfo.schema}/${this.tableInfo.table.tableName}/${this.column.columnName}`;
    const column = Object.assign({}, this.column);
    delete column.index;
    if(column.javaType == "default") {
      column.javaType = null;
    }
    this.http.put(url, { column: column, annotations: this.annotations.value }).subscribe(
      () => { this.notificationService.info(this.translate.instant("Column saved")); });
  }

  @Button({ list: "column", text: "Cancel" })
  cancelColumn() {
    this.column = null;
  }

  changeType(column, newType) {
    const url = `${this.portofino.apiRoot}portofino-upstairs/database/tables/${this.tableInfo.db}/${this.tableInfo.schema}/${this.tableInfo.table.tableName}/${this.column.columnName}/:annotations/${newType}`;
    this.http.get<{ classAccessor: ClassAccessor, annotations: any}>(url).subscribe(c => {
      const form = Form.fromClassAccessor(ClassAccessor.create(c.classAccessor));
      form.contents.forEach(f => {
        if(f instanceof Field) {
          f.initialState = c.annotations[f.name];
        }
      });
      this.annotationsForm = form;
    });
  }

  wizardStep(event) {
    console.log("event", event);
  }

}

class ConnectionProviderSummary {
  name: string;
  status: string;
  description: string;
}

class ConnectionProviderDetails {
  databaseName: { value: string };
  driver: { value: string };
  errorMessage: { value: string };
  falseString: { value: string };
  hibernateDialect: { value: string };
  jndiResource: { value: string };
  lastTested: { value: number; displayValue: string };
  password: { value: string };
  schemas: { catalog: string; name: string; schema: string; selected: boolean }[];
  status: { value: string };
  trueString: { value: string };
  url: { value: string };
  user: { value: string };
  username: { value: string };
}

class DatabasePlatform {
  description: string;
  standardDriverClassName: string;
  status: string;
}

class TableFlatNode {
  loading = false;
  readonly children: TableFlatNode[] = [];
  constructor(public db: string, public schema: {name: string, liquibase: boolean }, public table: string) {}

  get level() {
    if(this.table) {
      return 2;
    } else if(this.schema) {
      return 1;
    } else {
      return 0;
    }
  }

  get expandable() {
    return !this.table;
  }

  get displayName() {
    if(this.table) return this.table;
    if(this.schema) return this.schema.name;
    if(this.db) return this.db;
  }
}

class TableTreeDataSource {

  readonly dataChange = new BehaviorSubject<TableFlatNode[]>([]);

  get data(): TableFlatNode[] { return this.dataChange.value; }
  set data(value: TableFlatNode[]) {
    this.treeControl.dataNodes = value;
    this.dataChange.next(value);
  }

  constructor(private treeControl: FlatTreeControl<TableFlatNode>, private http: HttpClient, private apiRoot: string) {}

  connect(collectionViewer: CollectionViewer): Observable<TableFlatNode[]> {
    this.treeControl.expansionModel.changed.subscribe(change => {
      if ((change as SelectionChange<TableFlatNode>).added ||
        (change as SelectionChange<TableFlatNode>).removed) {
        this.handleTreeControl(change as SelectionChange<TableFlatNode>);
      }
    });

    return merge(collectionViewer.viewChange, this.dataChange).pipe(map(() => this.data));
  }

  /** Handle expand/collapse behaviors */
  handleTreeControl(change: SelectionChange<TableFlatNode>) {
    if (change.added) {
      change.added.forEach(node => this.toggleNode(node, true));
    }
    if (change.removed) {
      change.removed.slice().reverse().forEach(node => this.toggleNode(node, false));
    }
  }

  /**
   * Toggle the node, remove from display list
   */
  toggleNode(node: TableFlatNode, expand: boolean) {
    const index = this.data.indexOf(node);
    if (!node.expandable || index < 0) {
      return;
    }
    if(expand) {
      if(node.children.length > 0) {
        //Already loaded
        this.data.splice(index + 1, 0, ...node.children);
        this.dataChange.next(this.data);
      } else {
        this.loadNode(node, index);
      }
    } else {
      if(!node.schema) {
        node.children.forEach(n => { this.toggleNode(n, false); });
      }
      let count = 0;
      for (let i = index + 1; i < this.data.length && this.data[i].level > node.level; i++, count++) {}
      this.data.splice(index + 1, count);
      this.dataChange.next(this.data);
    }
  }

  protected loadNode(node: TableFlatNode, index) {
    node.loading = true;
    if(node.schema) {
      const url = `${this.apiRoot}portofino-upstairs/database/tables/${node.db}/${node.schema.name}`;
      this.http.get(url).subscribe((tables: any[]) => {
        tables.forEach(table => {
          node.children.push(new TableFlatNode(node.db, node.schema, table.name));
        });
        this.data.splice(index + 1, 0, ...node.children);
        node.loading = false;
        this.dataChange.next(this.data);
      });
    } else {
      const url = `${this.apiRoot}portofino-upstairs/database/connections/${node.db}`;
      this.http.get<ConnectionProviderDetails>(url).subscribe(c => {
        c.schemas.forEach(schema => {
          node.children.push(new TableFlatNode(node.db, { name: schema.schema, liquibase: false }, null)); //TODO
        });
        this.data.splice(index + 1, 0, ...node.children);
        node.loading = false;
        this.dataChange.next(this.data);
      });
    }
  }
}