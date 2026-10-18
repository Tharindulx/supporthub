export type Role='REQUESTER'|'AGENT'|'ADMIN';
export type Status='OPEN'|'IN_PROGRESS'|'WAITING'|'RESOLVED'|'CLOSED';
export type Priority='LOW'|'MEDIUM'|'HIGH'|'URGENT';
export interface Person {id:number;name:string;email:string;role:Role;enabled:boolean}
export interface Ticket {id:number;title:string;description:string;category:string;priority:Priority;status:Status;requester:Person;assignee:Person|null;createdAt:string;updatedAt:string;version:number}
export interface Detail {ticket:Ticket;comments:{id:number;author:Person;body:string;createdAt:string}[];activities:{id:number;actor:string;action:string;createdAt:string}[]}
export interface TicketPage {content:Ticket[];totalElements:number;totalPages:number;number:number}
export interface Dashboard {total:number;open:number;inProgress:number;waiting:number;resolved:number;closed:number;urgent:number}
