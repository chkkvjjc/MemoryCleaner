#include <unistd.h>
#include <string.h>
#include "jni.h"
#include "stdlib.h"
typedef unsigned char byte;
const char ldpath[] __attribute__ ((section(".interp")))="/system/bin/linker64";
int clean(long l){
    byte **b=NULL;
    long i=0,m=l/1024;
    b=(byte**)malloc(m*sizeof(byte*));
    while(i<m){
        *b=(byte*)malloc(1024*sizeof(byte));
        memset(*b,0,sizeof(*b));
        i++;*b++;
    }
    sleep(5);
    for(;i>0;*b--,i--){
        free(*b);
    }
    free(b);
    return 1;
}
void main(){
    FILE *file=fopen("/proc/self/cmdline","r");
    if(file==NULL)return;
    char *cmdline;
    cmdline=(char*)malloc(512*sizeof(char));
    char *c=NULL;
    while((*cmdline=fgetc(file))!=255){
        if(*(cmdline-1)=='\0')c=cmdline;
        cmdline++;
    }
    if(*c=='.')exit(0);
    clean(atol(c));
    exit(0);
}

JNIEXPORT jint JNICALL
Java_com_chkkvjjc_cleaner_MainActivity_cleaner(JNIEnv *env, jobject thiz, jlong l) {
    return clean(l);
}
