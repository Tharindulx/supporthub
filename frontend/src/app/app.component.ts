import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from './api.service';
import { DialogFocusDirective } from './dialog-focus.directive';
import { Dashboard, Detail, Person, Priority, Role, Status, Ticket, TicketPage } from './models';

@Component({selector:'app-root',standalone:true,imports:[CommonModule,FormsModule,DialogFocusDirective],templateUrl:'./app.component.html'})
export class AppComponent implements OnInit {
  api=inject(ApiService);cd=inject(ChangeDetectorRef);
  user:Person|null=null;ready=false;busy=false;error='';notice='';registering=false;
  email='';password='';name='';view:'tickets'|'team'='tickets';
  query='';status='';priority='';mine=false;page=0;
  tickets:TicketPage={content:[],totalElements:0,totalPages:0,number:0};
  stats:Dashboard={total:0,open:0,inProgress:0,waiting:0,resolved:0,closed:0,urgent:0};
  agents:Person[]=[];team:Person[]=[];selected:Detail|null=null;creating=false;commentText='';
  draft={title:'',description:'',category:'GENERAL',priority:'MEDIUM'};
  edit:{priority:Priority;status:Status;assigneeId:number|null;version:number}={priority:'MEDIUM',status:'OPEN',assigneeId:null,version:0};
  priorities:Priority[]=['LOW','MEDIUM','HIGH','URGENT'];statuses:Status[]=['OPEN','IN_PROGRESS','WAITING','RESOLVED','CLOSED'];
  categories=['GENERAL','SOFTWARE','HARDWARE','NETWORK','ACCESS'];roles:Role[]=['REQUESTER','AGENT','ADMIN'];
  get staff(){return !!this.user&&this.user.role!=='REQUESTER';}
  async ngOnInit(){
    try {await this.api.get('/auth/csrf');this.user=await this.api.get<Person>('/auth/me');await this.load();}
    catch(e:any){if(e.status!==401)this.error=e.error?.message||'Cannot connect to SupportHub. Check that the server is running.';}
    finally{this.ready=true;this.cd.detectChanges();}
  }
  async act(work:()=>Promise<void>){
    if(this.busy)return;this.busy=true;this.error='';this.notice='';
    try{await work();}catch(e:any){this.error=e.error?.message||e.error?.detail||'Something went wrong. Please try again.';if(e.status===401){this.user=null;this.selected=null;this.creating=false;}}
    finally{this.busy=false;this.cd.detectChanges();}
  }
  login(){void this.act(async()=>{
    await this.api.get('/auth/csrf');
    if(this.registering){await this.api.post('/auth/register',{name:this.name,email:this.email,password:this.password});}
    this.user=await this.api.post<Person>('/auth/login',{email:this.email,password:this.password});
    this.password='';this.view='tickets';this.query='';this.status='';this.priority='';this.mine=false;this.page=0;
    await this.api.get('/auth/csrf');await this.load();
  });}
  logout(){void this.act(async()=>{await this.api.post('/auth/logout',{});this.user=null;this.selected=null;this.creating=false;this.team=[];this.agents=[];await this.api.get('/auth/csrf');});}
  async load(){
    const params=new URLSearchParams({q:this.query,status:this.status,priority:this.priority,mine:String(this.mine),page:String(this.page)});
    if(!this.status)params.delete('status');if(!this.priority)params.delete('priority');
    const [tickets,stats]=await Promise.all([this.api.get<TicketPage>('/tickets?'+params),this.api.get<Dashboard>('/dashboard')]);
    this.tickets=tickets;this.stats=stats;
    if(this.staff)this.agents=await this.api.get<Person[]>('/agents');
  }
  filter(){this.page=0;void this.act(()=>this.load());}
  filterStatus(s:string){this.status=s;this.view='tickets';this.filter();}
  changePage(delta:number){this.page+=delta;void this.act(()=>this.load());}
  showTickets(mine=false){this.view='tickets';this.mine=mine;this.filter();}
  showTeam(){void this.act(async()=>{this.team=await this.api.get<Person[]>('/users');this.view='team';});}
  async fetchDetail(id:number){this.selected=await this.api.get<Detail>('/tickets/'+id);const t=this.selected.ticket;this.edit={priority:t.priority,status:t.status,assigneeId:t.assignee?.id??null,version:t.version};}
  open(t:Ticket){this.commentText='';void this.act(()=>this.fetchDetail(t.id));}
  refreshDetail(){if(this.selected)void this.act(()=>this.fetchDetail(this.selected!.ticket.id));}
  newTicket(){this.draft={title:'',description:'',category:'GENERAL',priority:'MEDIUM'};this.error='';this.creating=true;}
  create(){void this.act(async()=>{const t=await this.api.post<Ticket>('/tickets',this.draft);this.creating=false;this.query='';this.status='';this.priority='';this.mine=false;this.page=0;await this.load();await this.fetchDetail(t.id);this.notice='Ticket created. Your support team can now follow up.';});}
  save(){void this.act(async()=>{const id=this.selected!.ticket.id;await this.api.put('/tickets/'+id,this.edit);await this.fetchDetail(id);await this.load();this.notice='Ticket updated.';});}
  sendComment(){void this.act(async()=>{const id=this.selected!.ticket.id;await this.api.post('/tickets/'+id+'/comments',{body:this.commentText});this.commentText='';await this.fetchDetail(id);await this.load();this.notice='Comment added.';});}
  saveUser(person:Person){void this.act(async()=>{await this.api.put('/users/'+person.id,{role:person.role,enabled:person.enabled});this.team=await this.api.get<Person[]>('/users');this.agents=await this.api.get<Person[]>('/agents');this.notice='Team member updated.';});}
  label(s:string){return s.toLowerCase().replaceAll('_',' ').replace(/^./,c=>c.toUpperCase());}
  initials(name:string){return name.split(' ').map(n=>n[0]).slice(0,2).join('').toUpperCase();}
  nextStatuses():Status[]{
    if(!this.selected)return [];
    const map:Record<Status,Status[]>={OPEN:['OPEN','IN_PROGRESS','RESOLVED'],IN_PROGRESS:['IN_PROGRESS','WAITING','RESOLVED'],WAITING:['WAITING','IN_PROGRESS','RESOLVED'],RESOLVED:['RESOLVED','CLOSED','OPEN'],CLOSED:['CLOSED','OPEN']};
    return map[this.selected.ticket.status];
  }
}
