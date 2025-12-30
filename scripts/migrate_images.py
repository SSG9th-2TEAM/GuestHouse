"""
이미지 마이그레이션 스크립트
네이버 CDN URL에서 원본 이미지를 다운로드하여 Naver Object Storage에 업로드

대상 테이블:
1. accommodation_image (image_url)
2. room (main_image_url)
"""

import mysql.connector
import boto3
import requests
import urllib.parse
import uuid
import time
from botocore.config import Config

# ========== 설정 ==========
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 13306,
    'user': 'thismo',
    'password': 'thismo1234',
    'database': 'guesthouse'
}

NCLOUD_CONFIG = {
    'endpoint': 'https://kr.object.ncloudstorage.com',
    'region': 'kr-standard',
    'bucket': 'gusethouse',
    'access_key': 'ACCESS_KEY',
    'secret_key': 'SECRET_KEY'
}

# ========== S3 클라이언트 초기화 ==========
s3_client = boto3.client(
    's3',
    endpoint_url=NCLOUD_CONFIG['endpoint'],
    region_name=NCLOUD_CONFIG['region'],
    aws_access_key_id=NCLOUD_CONFIG['access_key'],
    aws_secret_access_key=NCLOUD_CONFIG['secret_key'],
    config=Config(signature_version='s3v4')
)

def extract_original_url(naver_cdn_url):
    """네이버 CDN URL에서 원본 이미지 URL 추출"""
    if 'src=' not in naver_cdn_url:
        return naver_cdn_url
    
    try:
        # src= 파라미터 추출
        src_part = naver_cdn_url.split('src=')[1]
        # URL 디코딩
        original_url = urllib.parse.unquote(src_part)
        return original_url
    except:
        return naver_cdn_url

def download_image(url):
    """이미지 다운로드"""
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.content
    except Exception as e:
        print(f"  다운로드 실패: {e}")
    return None

def get_extension(url, content_type=None):
    """URL 또는 Content-Type에서 확장자 추출"""
    if content_type:
        if 'png' in content_type:
            return 'png'
        elif 'gif' in content_type:
            return 'gif'
        elif 'webp' in content_type:
            return 'webp'
    
    # URL에서 추출
    lower_url = url.lower()
    if '.png' in lower_url:
        return 'png'
    elif '.gif' in lower_url:
        return 'gif'
    elif '.webp' in lower_url:
        return 'webp'
    
    return 'jpg'

def upload_to_storage(image_data, folder, extension='jpg'):
    """Naver Object Storage에 업로드"""
    try:
        filename = f"{folder}/{uuid.uuid4()}.{extension}"
        content_type = f"image/{extension}"
        
        s3_client.put_object(
            Bucket=NCLOUD_CONFIG['bucket'],
            Key=filename,
            Body=image_data,
            ContentType=content_type
            # ACL 제거 - 버킷 정책에서 공개 설정되어 있으면 별도 ACL 불필요
        )
        
        # 공개 URL 반환
        public_url = f"{NCLOUD_CONFIG['endpoint']}/{NCLOUD_CONFIG['bucket']}/{filename}"
        return public_url
    except Exception as e:
        print(f"  업로드 실패: {e}")
        return None

def migrate_table(cursor, conn, table_name, id_column, url_column, folder):
    """테이블의 이미지 마이그레이션"""
    print(f"\n{'='*50}")
    print(f"테이블: {table_name}")
    print(f"{'='*50}")
    
    # 네이버 CDN URL만 선택 (이미 마이그레이션된 것 제외)
    cursor.execute(f"""
        SELECT {id_column}, {url_column} 
        FROM {table_name} 
        WHERE {url_column} LIKE '%pstatic.net%'
        OR {url_column} LIKE '%search.pstatic.net%'
    """)
    
    rows = cursor.fetchall()
    total = len(rows)
    print(f"마이그레이션 대상: {total}개\n")
    
    success_count = 0
    fail_count = 0
    
    for i, (row_id, old_url) in enumerate(rows, 1):
        print(f"[{i}/{total}] ID={row_id}")
        
        if not old_url:
            print("  URL 없음, 스킵")
            continue
        
        # 원본 URL 추출
        original_url = extract_original_url(old_url)
        print(f"  원본 URL: {original_url[:80]}...")
        
        # 이미지 다운로드
        image_data = download_image(original_url)
        if not image_data:
            # 원본 URL 실패 시 CDN URL로 시도
            image_data = download_image(old_url)
            if not image_data:
                print("  다운로드 실패")
                fail_count += 1
                continue
        
        # 확장자 결정
        ext = get_extension(original_url)
        
        # 업로드
        new_url = upload_to_storage(image_data, folder, ext)
        if not new_url:
            fail_count += 1
            continue
        
        # DB 업데이트
        cursor.execute(f"""
            UPDATE {table_name} 
            SET {url_column} = %s 
            WHERE {id_column} = %s
        """, (new_url, row_id))
        conn.commit()
        
        print(f"  ✓ 완료: {new_url[:60]}...")
        success_count += 1
        
        # Rate limiting
        time.sleep(0.1)
    
    print(f"\n{table_name} 완료: 성공 {success_count}, 실패 {fail_count}")
    return success_count, fail_count

def main():
    print("="*60)
    print("이미지 마이그레이션 시작")
    print("네이버 CDN -> Naver Object Storage")
    print("="*60)
    
    # DB 연결
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    total_success = 0
    total_fail = 0
    
    try:
        # 1. accommodation_image 테이블
        s, f = migrate_table(
            cursor, conn,
            table_name='accommodation_image',
            id_column='image_id',
            url_column='image_url',
            folder='accommodation_image'
        )
        total_success += s
        total_fail += f
        
        # 2. room 테이블
        s, f = migrate_table(
            cursor, conn,
            table_name='room',
            id_column='room_id',
            url_column='main_image_url',
            folder='room'
        )
        total_success += s
        total_fail += f
        
    finally:
        cursor.close()
        conn.close()
    
    print("\n" + "="*60)
    print(f"전체 완료: 성공 {total_success}, 실패 {total_fail}")
    print("="*60)

if __name__ == '__main__':
    main()
